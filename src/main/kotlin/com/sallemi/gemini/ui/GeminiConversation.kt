package com.sallemi.gemini.ui

class GeminiConversation {
    data class Message(val role: String, val text: String)

    val messages: MutableList<Message> = mutableListOf()

    fun addUser(text: String) {
        messages.add(Message("user", text))
    }

    fun addAssistant(text: String) {
        messages.add(Message("model", text))
    }

    fun appendAssistantChunk(chunk: String) {
        val last = messages.lastOrNull()
        if (last == null || last.role != "model") {
            messages.add(Message("model", chunk))
        } else {
            messages[messages.lastIndex] = last.copy(text = last.text + chunk)
        }
    }
}
