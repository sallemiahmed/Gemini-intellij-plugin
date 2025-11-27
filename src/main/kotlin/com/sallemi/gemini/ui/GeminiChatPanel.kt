package com.sallemi.gemini.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.ui.EditorTextField
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.sallemi.gemini.services.GeminiService
import com.sallemi.gemini.settings.GeminiSettingsState
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.*

/**
 * Main chat panel with native IntelliJ UI components
 * Completely redesigned to look like JetBrains AI Assistant
 */
class GeminiChatPanel(private val project: Project) {

    val component: JComponent
    private var messagesPanel: JPanel? = null
    private var scrollPane: JBScrollPane? = null
    private var inputField: EditorTextField? = null
    private var sendButton: JButton? = null
    private var statusLabel: JBLabel? = null
    private var currentStreamingMessage: ChatMessageComponent? = null

    init {
        // Check if API key is configured
        val hasApiKey = try {
            GeminiSettingsState.getInstance().state.apiKey?.isNotBlank() == true
        } catch (e: Exception) {
            false
        }

        component = if (!hasApiKey) {
            // Show onboarding screen
            EmptyStatePanel(project)
        } else {
            // Build main chat UI
            buildChatUI()
        }
    }

    private fun buildChatUI(): JComponent {
        val mainPanel = JPanel(BorderLayout()).apply {
            background = UIUtil.getPanelBackground()
        }

        // Messages area
        messagesPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = UIUtil.getPanelBackground()
            border = JBUI.Borders.empty(8)
        }

        scrollPane = JBScrollPane(messagesPanel).apply {
            border = JBUI.Borders.empty()
            verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        }

        mainPanel.add(scrollPane, BorderLayout.CENTER)

        // Bottom input area
        val inputPanel = createInputPanel()
        mainPanel.add(inputPanel, BorderLayout.SOUTH)

        return mainPanel
    }

    private fun createInputPanel(): JComponent {
        val panel = JPanel(BorderLayout()).apply {
            background = UIUtil.getPanelBackground()
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, JBColor.border()),
                JBUI.Borders.empty(8)
            )
        }

        // Status label (top of input area)
        statusLabel = JBLabel("").apply {
            font = JBUI.Fonts.smallFont()
            foreground = JBColor.GRAY
            border = JBUI.Borders.empty(0, 0, 4, 0)
        }

        // Input field (multiline)
        inputField = EditorTextField("", project, PlainTextFileType.INSTANCE).apply {
            setOneLineMode(false)
            setPlaceholder("Ask Gemini anything...")
            preferredSize = Dimension(preferredSize.width, 60)

            // Add Enter key handler (Ctrl+Enter or Cmd+Enter to send)
            addSettingsProvider { editor ->
                editor.contentComponent.addKeyListener(object : KeyAdapter() {
                    override fun keyPressed(e: KeyEvent) {
                        if (e.keyCode == KeyEvent.VK_ENTER) {
                            if (e.isControlDown || e.isMetaDown) {
                                e.consume()
                                sendMessage()
                            }
                        }
                    }
                })
            }
        }

        // Send button
        sendButton = JButton("Send").apply {
            preferredSize = Dimension(80, 36)
            addActionListener { sendMessage() }
        }

        // Input row (field + button)
        val inputRow = JPanel(BorderLayout(8, 0)).apply {
            background = UIUtil.getPanelBackground()
            add(inputField, BorderLayout.CENTER)
            add(sendButton, BorderLayout.EAST)
        }

        // Combine status + input
        val inputContainer = JPanel(BorderLayout()).apply {
            background = UIUtil.getPanelBackground()
            add(statusLabel, BorderLayout.NORTH)
            add(inputRow, BorderLayout.CENTER)
        }

        panel.add(inputContainer, BorderLayout.CENTER)

        // Hint label
        val hintLabel = JBLabel("Press Ctrl+Enter to send").apply {
            font = JBUI.Fonts.smallFont()
            foreground = JBColor.GRAY
            border = JBUI.Borders.empty(4, 0, 0, 0)
        }
        panel.add(hintLabel, BorderLayout.SOUTH)

        return panel
    }

    private fun sendMessage() {
        val text = inputField?.text?.trim() ?: return
        if (text.isBlank()) return

        // Clear input
        ApplicationManager.getApplication().invokeLater {
            inputField?.text = ""
        }

        // Send to service
        GeminiService.getInstance(project).handleChatMessage(text)
    }

    fun addUserMessage(text: String) {
        ApplicationManager.getApplication().invokeLater {
            messagesPanel?.let { panel ->
                val messageComponent = ChatMessageComponent(text, isAssistant = false, project = project)
                panel.add(messageComponent)
                panel.revalidate()
                panel.repaint()
                scrollToBottom()
            }
        }
    }

    fun addAssistantMessage(text: String) {
        ApplicationManager.getApplication().invokeLater {
            messagesPanel?.let { panel ->
                val messageComponent = ChatMessageComponent(text, isAssistant = true, project = project)
                panel.add(messageComponent)
                panel.revalidate()
                panel.repaint()
                scrollToBottom()
            }
        }
    }

    fun startAssistantMessage() {
        ApplicationManager.getApplication().invokeLater {
            messagesPanel?.let { panel ->
                val messageComponent = ChatMessageComponent("", isAssistant = true, project = project)
                messageComponent.startStreaming()
                currentStreamingMessage = messageComponent
                panel.add(messageComponent)
                panel.revalidate()
                panel.repaint()
                scrollToBottom()
            }
        }
    }

    fun appendAssistantChunk(text: String) {
        ApplicationManager.getApplication().invokeLater {
            currentStreamingMessage?.appendText(text)
            scrollToBottom()
        }
    }

    fun finishAssistantMessage() {
        ApplicationManager.getApplication().invokeLater {
            currentStreamingMessage?.stopStreaming()
            currentStreamingMessage = null
        }
    }

    fun setStatus(status: String) {
        ApplicationManager.getApplication().invokeLater {
            statusLabel?.text = status
            statusLabel?.isVisible = status.isNotBlank()
        }
    }

    fun clearChat() {
        ApplicationManager.getApplication().invokeLater {
            messagesPanel?.let { panel ->
                panel.removeAll()
                panel.revalidate()
                panel.repaint()
                currentStreamingMessage = null
                GeminiService.getInstance(project).clearConversation()
            }
        }
    }

    private fun scrollToBottom() {
        SwingUtilities.invokeLater {
            // Force layout update first
            messagesPanel?.revalidate()
            messagesPanel?.repaint()

            // Then scroll to bottom using invokeLater to ensure layout is complete
            SwingUtilities.invokeLater {
                scrollPane?.verticalScrollBar?.let { bar ->
                    // Set to maximum value plus a small buffer
                    bar.value = bar.maximum + bar.visibleAmount
                }
            }
        }
    }

    fun refresh() {
        // Reload if API key state changed
        val parent = component.parent
        if (parent != null) {
            val hasApiKey = try {
                GeminiSettingsState.getInstance().state.apiKey?.isNotBlank() == true
            } catch (e: Exception) {
                false
            }
            val isShowingEmpty = component is EmptyStatePanel

            if (hasApiKey && isShowingEmpty) {
                // Need to rebuild with chat UI
                parent.remove(component)
                val newPanel = GeminiChatPanel(project)
                GeminiService.getInstance(project).attachPanel(newPanel)
                parent.add(newPanel.component)
                parent.revalidate()
                parent.repaint()
            }
        }
    }
}
