package com.sallemi.gemini.services

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.sallemi.gemini.api.GeminiClient
import com.sallemi.gemini.settings.GeminiSettingsState
import com.sallemi.gemini.ui.GeminiChatPanel
import com.sallemi.gemini.ui.GeminiConversation

@Service(Service.Level.PROJECT)
class GeminiService(private val project: Project) {

    private val conversation = GeminiConversation()
    private var chatPanel: GeminiChatPanel? = null
    private var isGenerating = false
    private var shouldStop = false

    fun attachPanel(panel: GeminiChatPanel) {
        chatPanel = panel
        // Restore conversation history if any
        if (conversation.messages.isNotEmpty()) {
            conversation.messages.forEach { message ->
                if (message.role == "user") {
                    panel.addUserMessage(message.text)
                } else {
                    panel.addAssistantMessage(message.text)
                }
            }
        }
    }

    fun handleChatMessage(message: String) {
        if (message.isBlank()) return
        send(ProgrammingQuestionIntent, message)
    }

    fun openChat() {
        openToolWindow()
        ApplicationManager.getApplication().invokeLater {
            chatPanel?.setStatus("Ready")
        }
    }

    fun sendIntent(intent: GeminiIntent, content: String, context: String? = null) {
        if (content.isBlank()) return
        val prompt = intent.buildPrompt(content, context)
        send(intent, prompt)
    }

    fun clearConversation() {
        conversation.messages.clear()
    }

    fun isGenerating(): Boolean = isGenerating

    fun stopGeneration() {
        shouldStop = true
    }

    private fun send(intent: GeminiIntent, prompt: String) {
        openToolWindow()
        conversation.addUser(prompt)

        ApplicationManager.getApplication().invokeLater {
            chatPanel?.addUserMessage(prompt)
            chatPanel?.setStatus("Working on ${intent.title}...")
        }

        val settingsState = GeminiSettingsState.getInstance()
        val client = GeminiClient(settingsState.state)

        isGenerating = true
        shouldStop = false

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                if (settingsState.state.streaming) {
                    // Start streaming message with cursor
                    ApplicationManager.getApplication().invokeLater {
                        chatPanel?.startAssistantMessage()
                    }

                    client.generateContent(conversation) { chunk ->
                        if (shouldStop) {
                            return@generateContent
                        }
                        conversation.appendAssistantChunk(chunk)
                        ApplicationManager.getApplication().invokeLater {
                            chatPanel?.appendAssistantChunk(chunk)
                        }
                    }

                    // Finish streaming
                    ApplicationManager.getApplication().invokeLater {
                        chatPanel?.finishAssistantMessage()
                    }
                } else {
                    val response = client.generateContent(conversation)
                    conversation.addAssistant(response)
                    ApplicationManager.getApplication().invokeLater {
                        chatPanel?.addAssistantMessage(response)
                    }
                }
            } catch (e: Exception) {
                val message = if (shouldStop) {
                    "[Generation stopped by user]"
                } else {
                    "Gemini request failed: ${e.message}"
                }
                conversation.addAssistant(message)
                ApplicationManager.getApplication().invokeLater {
                    // If streaming was started, append error to the streaming message
                    // instead of creating a new empty bubble
                    if (settingsState.state.streaming) {
                        chatPanel?.appendAssistantChunk(message)
                        chatPanel?.finishAssistantMessage()
                    } else {
                        chatPanel?.addAssistantMessage(message)
                    }
                }
            } finally {
                isGenerating = false
                shouldStop = false
                ApplicationManager.getApplication().invokeLater {
                    chatPanel?.setStatus("")
                }
            }
        }
    }

    private fun openToolWindow() {
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Gemini")
        toolWindow?.activate(null, true)
    }

    companion object {
        fun getInstance(project: Project): GeminiService =
            project.getService(GeminiService::class.java)
    }
}
