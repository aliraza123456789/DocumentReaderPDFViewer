package com.pdf.alldocument.viewer.pdfreaderapp.presentation.tools.aireading

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdf.alldocument.viewer.pdfreaderapp.BuildConfig
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
    SUMMARIZE("Summarize Document", "Summarize this document in a clear, concise way with a short overview and bullet points."),
    KEY_TAKEAWAYS("Key Takeaways", "Extract the key takeaways from this document as a numbered list."),
    EXPLAIN_TERMS("Explain Complex Terms", "Identify complex or technical terms in this document and explain each in simple language."),
    QA("Generate Q&A", "Generate 8 useful question-and-answer pairs based on this document.")
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
                //chat = buildModel(text).startChat()
                _uiState.update { it.copy(isExtracting = false, documentChars = full.length, truncated = truncated) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isExtracting = false, error = e.message ?: "Failed to read document") }
            }
        }
    }

    private fun extractPdfText(uri: Uri): String =
        context.contentResolver.openInputStream(uri)!!.use { input ->
            PDDocument.load(input).use { doc -> PDFTextStripper().getText(doc) }
        }

 /*   private fun buildModel(documentText: String) = GenerativeModel(
        modelName = MODEL,
        apiKey = BuildConfig.GEMINI_API_KEY,
        generationConfig = generationConfig { temperature = 0.4f },
        systemInstruction = content {
            text(
                "You are a document reading assistant. Answer ONLY using the document below. " +
                "If the answer is not in the document, say so. Reply in the user's language.\n\n" +
                "=== DOCUMENT START ===\n$documentText\n=== DOCUMENT END ==="
            )
        }
    )*/

    fun runQuickAction(a: QuickAction) = send(a.prompt, displayText = a.label)

    fun send(prompt: String, displayText: String = prompt) {
        val c = chat ?: return
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
