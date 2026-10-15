package com.pdf.alldocument.office.pdfreaderapp.presentation.tools.aireading

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdf.alldocument.office.pdfreaderapp.BuildConfig
import com.google.ai.client.generativeai.Chat
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ChatMessage(val id: Long, val fromUser: Boolean, val text: String)

data class AiReadingUiState(
    val documentName: String? = null,
    val documentChars: Int = 0,
    val truncated: Boolean = false,
    val isExtracting: Boolean = false,
    val isResponding: Boolean = false,
    val messages: List<ChatMessage> = emptyList(),
    val error: String? = null
)

enum class QuickAction(val label: String, val prompt: String) {
    SUMMARIZE("Summarize", "Summarize this document in a clear, concise way with an overview and key points."),
    STUDY_NOTES("Study Notes", "Generate comprehensive study & revision notes from this document with bullet points, key definitions, and formulas."),
    QA("Generate Quiz", "Generate 5-8 useful quiz questions (with answers and explanations) based on this document."),
    KEY_TAKEAWAYS("Key Takeaways", "Extract the key takeaways and important points from this document as a numbered list."),
    EXPLAIN_TERMS("Explain Terms", "Identify complex or technical terms in this document and explain each in simple, easy-to-understand language.")
}

@HiltViewModel
class AiReadingViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private companion object {
        const val MODEL = "gemini-2.0-flash"
        /** ~ 60k chars keeps us well inside the context window and bounds cost. */
        const val MAX_DOC_CHARS = 60_000
    }

    private val _uiState = MutableStateFlow(AiReadingUiState())
    val uiState: StateFlow<AiReadingUiState> = _uiState

    private var chat: Chat? = null
    private var job: Job? = null
    private var nextId = 0L

    init { PDFBoxResourceLoader.init(context) }

    fun loadDocument(uri: Uri, name: String) {
        _uiState.update { AiReadingUiState(documentName = name, isExtracting = true) }
        viewModelScope.launch {
            try {
                val full = withContext(Dispatchers.IO) { extractPdfText(uri) }
                if (full.isBlank()) {
                    _uiState.update { it.copy(isExtracting = false, error = "No selectable text found (scanned PDF? run OCR first).") }
                    return@launch
                }
                val truncated = full.length > MAX_DOC_CHARS
                val text = if (truncated) full.take(MAX_DOC_CHARS) else full
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isBlank()) {
                    _uiState.update {
                        it.copy(
                            isExtracting = false,
                            documentChars = full.length,
                            truncated = truncated,
                            error = "GEMINI_API_KEY missing. Please add GEMINI_API_KEY in local.properties"
                        )
                    }
                    return@launch
                }
                chat = buildModel(text, apiKey).startChat()
                _uiState.update { it.copy(isExtracting = false, documentChars = full.length, truncated = truncated, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isExtracting = false, error = e.message ?: "Failed to read document") }
            }
        }
    }

    private fun extractPdfText(uri: Uri): String =
        context.contentResolver.openInputStream(uri)!!.use { input ->
            PDDocument.load(input).use { doc -> PDFTextStripper().getText(doc) }
        }

    private fun buildModel(documentText: String, apiKey: String) = GenerativeModel(
        modelName = MODEL,
        apiKey = apiKey,
        generationConfig = generationConfig { temperature = 0.4f },
        systemInstruction = content {
            text(
                "You are an expert document reading assistant and study helper. Answer clearly, accurately, and helpfully using the document below. " +
                "If the answer is not in the document, say so politely. Reply in the user's language.\n\n" +
                "=== DOCUMENT START ===\n$documentText\n=== DOCUMENT END ==="
            )
        }
    )

    fun runQuickAction(a: QuickAction) = send(a.prompt, displayText = a.label)

    fun send(prompt: String, displayText: String = prompt) {
        val c = chat
        if (c == null) {
            val key = BuildConfig.GEMINI_API_KEY
            if (key.isBlank()) {
                _uiState.update { it.copy(error = "GEMINI_API_KEY missing in local.properties") }
            } else if (_uiState.value.documentName == null) {
                _uiState.update { it.copy(error = "Please open a document first") }
            }
            return
        }
        if (prompt.isBlank() || _uiState.value.isResponding) return
        val userMsg = ChatMessage(nextId++, true, displayText)
        val aiId = nextId++
        _uiState.update { it.copy(messages = it.messages + userMsg + ChatMessage(aiId, false, ""), isResponding = true, error = null) }

        job = viewModelScope.launch {
            try {
                val sb = StringBuilder()
                c.sendMessageStream(prompt).collect { chunk ->
                    chunk.text?.let {
                        sb.append(it)
                        val snapshot = sb.toString()
                        _uiState.update { s -> s.copy(messages = s.messages.map { m -> if (m.id == aiId) m.copy(text = snapshot) else m }) }
                    }
                }
                _uiState.update { it.copy(isResponding = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isResponding = false, error = e.message ?: "AI request failed") }
            }
        }
    }

    fun stop() { job?.cancel(); _uiState.update { it.copy(isResponding = false) } }
}
