package com.pdf.alldocument.office.pdfreaderapp.presentation.tools.aireading

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pdf.alldocument.office.pdfreaderapp.BuildConfig
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
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
        const val MODEL = "llama-3.3-70b-versatile"
        const val GROQ_URL = "https://api.groq.com/openai/v1/chat/completions"
        /** ~ 60k chars keeps us well inside the context window and bounds cost. */
        const val MAX_DOC_CHARS = 60_000
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val _uiState = MutableStateFlow(AiReadingUiState())
    val uiState: StateFlow<AiReadingUiState> = _uiState

    private var documentText: String? = null
    private var activeCall: Call? = null
    private var job: Job? = null
    private var nextId = 0L

    init {
        PDFBoxResourceLoader.init(context)
    }

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
                val apiKey = BuildConfig.GROQ_API_KEY
                if (apiKey.isBlank()) {
                    _uiState.update {
                        it.copy(
                            isExtracting = false,
                            documentChars = full.length,
                            truncated = truncated,
                            error = "GROQ_API_KEY missing. Please add GROQ_API_KEY in local.properties"
                        )
                    }
                    return@launch
                }
                documentText = text
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

    fun runQuickAction(a: QuickAction) = send(a.prompt, displayText = a.label)

    fun send(prompt: String, displayText: String = prompt) {
        val text = documentText
        if (text == null) {
            val key = BuildConfig.GROQ_API_KEY
            if (key.isBlank()) {
                _uiState.update { it.copy(error = "GROQ_API_KEY missing in local.properties") }
            } else if (_uiState.value.documentName == null) {
                _uiState.update { it.copy(error = "Please open a document first") }
            }
            return
        }
        if (prompt.isBlank() || _uiState.value.isResponding) return

        val apiKey = BuildConfig.GROQ_API_KEY
        if (apiKey.isBlank()) {
            _uiState.update { it.copy(error = "GROQ_API_KEY missing in local.properties") }
            return
        }

        val userMsg = ChatMessage(nextId++, true, displayText)
        val aiId = nextId++
        val currentMessages = _uiState.value.messages
        _uiState.update {
            it.copy(
                messages = currentMessages + userMsg + ChatMessage(aiId, false, ""),
                isResponding = true,
                error = null
            )
        }

        job = viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonBody = JSONObject().apply {
                    put("model", MODEL)
                    put("temperature", 0.4)
                    put("stream", true)

                    val messagesArr = JSONArray()
                    messagesArr.put(JSONObject().apply {
                        put("role", "system")
                        put(
                            "content",
                            "You are an expert document reading assistant and study helper. Answer clearly, accurately, and helpfully using the document below. " +
                            "If the answer is not in the document, say so politely. Reply in the user's language.\n\n" +
                            "=== DOCUMENT START ===\n$text\n=== DOCUMENT END ==="
                        )
                    })

                    // Retain up to the last 10 messages for conversation context
                    val history = currentMessages.takeLast(10)
                    for (m in history) {
                        if (m.text.isNotBlank()) {
                            messagesArr.put(JSONObject().apply {
                                put("role", if (m.fromUser) "user" else "assistant")
                                put("content", m.text)
                            })
                        }
                    }

                    messagesArr.put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })

                    put("messages", messagesArr)
                }

                val request = Request.Builder()
                    .url(GROQ_URL)
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("Content-Type", "application/json")
                    .post(jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()

                val call = httpClient.newCall(request)
                activeCall = call

                call.execute().use { response ->
                    if (!response.isSuccessful) {
                        val errString = response.body?.string().orEmpty()
                        val errorMessage = try {
                            val errObj = JSONObject(errString).optJSONObject("error")
                            errObj?.optString("message") ?: "Groq API error (${response.code})"
                        } catch (_: Exception) {
                            "Groq API error: ${response.code} ${response.message}"
                        }
                        _uiState.update { it.copy(isResponding = false, error = errorMessage) }
                        return@use
                    }

                    val source = response.body?.source()
                    if (source == null) {
                        _uiState.update { it.copy(isResponding = false, error = "Empty response from server") }
                        return@use
                    }

                    val sb = StringBuilder()
                    while (!source.exhausted()) {
                        val line = source.readUtf8Line() ?: break
                        val trimmed = line.trim()
                        if (trimmed.startsWith("data:")) {
                            val data = trimmed.removePrefix("data:").trim()
                            if (data == "[DONE]") break
                            if (data.isNotEmpty()) {
                                try {
                                    val chunkObj = JSONObject(data)
                                    val choices = chunkObj.optJSONArray("choices")
                                    val delta = choices?.optJSONObject(0)?.optJSONObject("delta")
                                    val content = delta?.optString("content")?.takeIf { it.isNotEmpty() }
                                    if (!content.isNullOrEmpty()) {
                                        sb.append(content)
                                        val snapshot = sb.toString()
                                        _uiState.update { s ->
                                            s.copy(messages = s.messages.map { m ->
                                                if (m.id == aiId) m.copy(text = snapshot) else m
                                            })
                                        }
                                    }
                                } catch (_: Exception) {
                                    // Ignore heartbeat or partial chunk json issues
                                }
                            }
                        }
                    }
                    _uiState.update { it.copy(isResponding = false) }
                }
            } catch (e: Exception) {
                if (e is CancellationException || activeCall?.isCanceled() == true) {
                    _uiState.update { it.copy(isResponding = false) }
                } else {
                    _uiState.update { it.copy(isResponding = false, error = e.message ?: "AI request failed") }
                }
            } finally {
                activeCall = null
            }
        }
    }

    fun stop() {
        activeCall?.cancel()
        job?.cancel()
        _uiState.update { it.copy(isResponding = false) }
    }

    override fun onCleared() {
        super.onCleared()
        stop()
    }
}
