package com.pdf.alldocument.office.pdfreaderapp.presentation.tools.unlock

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdf.alldocument.office.pdfreaderapp.core.util.FileUtils
import com.pdf.alldocument.office.pdfreaderapp.data.pdftools.repository.PdfRepository
import com.pdf.alldocument.office.pdfreaderapp.domain.model.DocumentItem
import com.pdf.alldocument.office.pdfreaderapp.domain.model.DocumentType
import com.pdf.alldocument.office.pdfreaderapp.domain.model.SortOrder
import com.pdf.alldocument.office.pdfreaderapp.domain.usecase.GetDocumentsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

enum class UnlockFlowStep {
    SELECT_FILE,
    SUCCESS
}

data class UnlockPdfUiState(
    val step: UnlockFlowStep = UnlockFlowStep.SELECT_FILE,
    val availablePdfs: List<DocumentItem> = emptyList(),
    val filteredPdfs: List<DocumentItem> = emptyList(),
    val isLoading: Boolean = false,
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val selectedDoc: DocumentItem? = null,
    val showPasswordSheet: Boolean = false,
    val passwordInput: String = "",
    val isPasswordVisible: Boolean = false,
    val isUnlocking: Boolean = false,
    val unlockProgress: Int = 0,
    val unlockedFile: File? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class UnlockPdfViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pdfRepository: PdfRepository,
    private val getDocumentsUseCase: GetDocumentsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(UnlockPdfUiState())
    val state: StateFlow<UnlockPdfUiState> = _state.asStateFlow()

    init {
        loadDevicePdfs()
    }

    fun loadDevicePdfs() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            getDocumentsUseCase(DocumentType.PDF, SortOrder.DATE_DESC).collect { docs ->
                // Check all scanned documents + direct app output directories to find locked PDFs
                val lockedDocs = withContext(Dispatchers.IO) {
                    val allCandidates = docs.toMutableList()

                    // Also scan output folders directly for recently locked files
                    val folders = listOfNotNull(
                        FileUtils.getOutputDocumentsFolder(),
                        FileUtils.outputDir(context)
                    ).filter { it.exists() && it.isDirectory }

                    for (folder in folders) {
                        val pdfFiles = folder.listFiles { file -> file.extension.equals("pdf", ignoreCase = true) }
                        if (pdfFiles != null) {
                            for (f in pdfFiles) {
                                if (allCandidates.none { it.path == f.absolutePath }) {
                                    val size = f.length()
                                    allCandidates.add(
                                        DocumentItem(
                                            path = f.absolutePath,
                                            name = f.name,
                                            size = size,
                                            formattedSize = FileUtils.formatFileSize(size),
                                            dateModified = f.lastModified(),
                                            formattedDate = FileUtils.formatDate(f.lastModified()),
                                            extension = "pdf",
                                            type = DocumentType.PDF
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Filter down strictly to PDFs that have a password / are encrypted
                    allCandidates
                        .distinctBy { it.path }
                        .filter { doc -> isPdfLocked(doc.path) }
                        .sortedByDescending { it.dateModified }
                }

                _state.update { current ->
                    val filtered = if (current.searchQuery.isBlank()) lockedDocs
                    else lockedDocs.filter { it.name.contains(current.searchQuery, ignoreCase = true) }
                    current.copy(
                        availablePdfs = lockedDocs,
                        filteredPdfs = filtered,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun isPdfLocked(path: String): Boolean {
        val file = File(path)
        if (!file.exists() && !path.startsWith("content://")) return false

        // 1. Check with iText
        try {
            val stream = if (path.startsWith("content://")) {
                context.contentResolver.openInputStream(Uri.parse(path)) ?: return false
            } else {
                java.io.FileInputStream(file)
            }
            stream.use { s ->
                val reader = com.itextpdf.text.pdf.PdfReader(s)
                val enc = reader.isEncrypted
                reader.close()
                if (enc) return true
            }
        } catch (e: com.itextpdf.text.exceptions.BadPasswordException) {
            // Thrown when an open password is required
            return true
        } catch (e: Exception) {
            val msg = e.message?.lowercase() ?: ""
            if (msg.contains("password") || msg.contains("badpassword") || msg.contains("encrypted")) {
                return true
            }
        }

        // 2. Check with PDFBox Android
        try {
            com.tom_roush.pdfbox.android.PDFBoxResourceLoader.init(context)
            val doc = if (path.startsWith("content://")) {
                val stream = context.contentResolver.openInputStream(Uri.parse(path)) ?: return false
                com.tom_roush.pdfbox.pdmodel.PDDocument.load(stream)
            } else {
                com.tom_roush.pdfbox.pdmodel.PDDocument.load(file)
            }
            val enc = doc.isEncrypted
            doc.close()
            if (enc) return true
        } catch (e: com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException) {
            return true
        } catch (e: Exception) {
            val msg = e.message?.lowercase() ?: ""
            if (msg.contains("password") || msg.contains("encrypted")) {
                return true
            }
        }

        return false
    }

    fun setSearchQuery(query: String) {
        _state.update { current ->
            val filtered = if (query.isBlank()) current.availablePdfs
            else current.availablePdfs.filter { it.name.contains(query, ignoreCase = true) }
            current.copy(searchQuery = query, filteredPdfs = filtered)
        }
    }

    fun toggleSearch(open: Boolean) {
        _state.update { current ->
            current.copy(
                isSearching = open,
                searchQuery = if (!open) "" else current.searchQuery,
                filteredPdfs = if (!open) current.availablePdfs else current.filteredPdfs
            )
        }
    }

    fun selectDocument(doc: DocumentItem) {
        _state.update {
            it.copy(
                selectedDoc = doc,
                showPasswordSheet = true,
                passwordInput = "",
                isPasswordVisible = false,
                errorMessage = null
            )
        }
    }

    fun selectCustomUri(uri: Uri) {
        val temp = FileUtils.copyUriToTempFile(context, uri, "unlock_${System.currentTimeMillis()}.pdf")
        if (temp != null) {
            if (!isPdfLocked(temp.absolutePath)) {
                _state.update { it.copy(errorMessage = "This PDF is not password protected. Only locked PDFs can be unlocked.") }
                return
            }

            val size = temp.length()
            val doc = DocumentItem(
                path = temp.absolutePath,
                name = temp.name,
                size = size,
                formattedSize = FileUtils.formatFileSize(size),
                dateModified = temp.lastModified(),
                formattedDate = FileUtils.formatDate(temp.lastModified()),
                extension = FileUtils.getFileExtension(temp.name),
                type = DocumentType.PDF
            )
            selectDocument(doc)
        } else {
            _state.update { it.copy(errorMessage = "Could not load selected PDF file") }
        }
    }

    fun updatePassword(password: String) {
        _state.update { it.copy(passwordInput = password) }
    }

    fun togglePasswordVisibility() {
        _state.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun dismissPasswordSheet() {
        _state.update { it.copy(showPasswordSheet = false, selectedDoc = null, passwordInput = "") }
    }

    fun clearErrorMessage() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun executeUnlock() {
        val current = _state.value
        val doc = current.selectedDoc ?: return
        val password = current.passwordInput
        if (password.isBlank()) {
            _state.update { it.copy(errorMessage = "Please enter the password") }
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    showPasswordSheet = false,
                    isUnlocking = true,
                    unlockProgress = 10,
                    errorMessage = null
                )
            }

            try {
                // Progressive progress animation
                for (p in 15..70 step 15) {
                    delay(50)
                    _state.update { it.copy(unlockProgress = p) }
                }

                val file = File(doc.path)
                val sourceUri = if (file.exists()) {
                    Uri.fromFile(file)
                } else if (doc.path.startsWith("content://")) {
                    Uri.parse(doc.path)
                } else {
                    Uri.fromFile(file)
                }

                val baseName = doc.name.substringBeforeLast('.', "Document")
                val cleanName = if (baseName.endsWith("_locked", ignoreCase = true)) {
                    baseName.substringBeforeLast("_locked")
                } else {
                    "${baseName}_unlocked"
                }

                val publicDir = FileUtils.getOutputDocumentsFolder()
                val outputDir = if (publicDir.exists() || publicDir.mkdirs()) publicDir else FileUtils.outputDir(context)
                val outputFile = File(outputDir, "${cleanName}.pdf")

                pdfRepository.unlockPdf(sourceUri, outputFile, password)

                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(outputFile.absolutePath),
                    arrayOf("application/pdf"),
                    null
                )

                for (p in 75..100 step 10) {
                    delay(30)
                    _state.update { it.copy(unlockProgress = p) }
                }

                _state.update {
                    it.copy(
                        isUnlocking = false,
                        unlockProgress = 100,
                        unlockedFile = outputFile,
                        step = UnlockFlowStep.SUCCESS
                    )
                }

                // Refresh locked list
                loadDevicePdfs()
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isUnlocking = false,
                        showPasswordSheet = true, // re-open sheet so user can re-try password
                        errorMessage = "Incorrect password. Please try again."
                    )
                }
            }
        }
    }

    fun resetFlow() {
        _state.update {
            it.copy(
                step = UnlockFlowStep.SELECT_FILE,
                selectedDoc = null,
                showPasswordSheet = false,
                passwordInput = "",
                isUnlocking = false,
                unlockedFile = null,
                errorMessage = null
            )
        }
        loadDevicePdfs()
    }

    fun handleBackPress(): Boolean {
        val current = _state.value
        return when {
            current.isUnlocking -> true
            current.showPasswordSheet -> {
                dismissPasswordSheet()
                true
            }
            current.isSearching -> {
                toggleSearch(false)
                true
            }
            current.step == UnlockFlowStep.SUCCESS -> {
                resetFlow()
                false
            }
            else -> false
        }
    }
}
