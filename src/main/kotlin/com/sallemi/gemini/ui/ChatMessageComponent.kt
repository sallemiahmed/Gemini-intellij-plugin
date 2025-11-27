package com.sallemi.gemini.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.swing.*

/**
 * Individual chat message bubble with support for syntax-highlighted code blocks.
 *
 * Features:
 * - User messages: simple text bubbles (right-aligned)
 * - Assistant messages: mixed text and syntax-highlighted code blocks (left-aligned)
 * - Streaming support with blinking cursor
 * - Theme-aware colors
 */
class ChatMessageComponent(
    private val message: String,
    private val isAssistant: Boolean,
    private val timestamp: LocalDateTime = LocalDateTime.now(),
    private val project: Project? = null
) : JPanel(), Disposable {

    private var isStreaming = false
    private val cursorTimer: Timer
    private var rawContent: String = message
    private val codeBlocks = mutableListOf<CodeBlockComponent>()

    // Content panel that holds all segments
    private val contentPanel: JPanel

    // For simple text display (user messages or plain assistant messages)
    private var simpleTextArea: JTextArea? = null

    // For streaming: we need to track the last text segment for cursor animation
    private var streamingTextArea: JTextArea? = null

    companion object {
        private val TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm")

        // Theme-aware colors for Darcula support
        val ASSISTANT_BG = JBColor(0xF0F4F8, 0x2B2D30)  // Lighter for readability
        val ASSISTANT_FG = JBColor.foreground()
        val USER_BG = JBColor(0x2F65D3, 0x3A7AFE)  // Blue
        val USER_FG = JBColor.WHITE
        val TIMESTAMP_FG = JBColor.GRAY
    }

    init {
        layout = BorderLayout()
        isOpaque = false
        border = JBUI.Borders.empty(JBUI.scale(4), JBUI.scale(8))

        // Content panel for message content
        contentPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
        }

        // Build the message UI
        buildMessageUI()

        // Blinking cursor timer for streaming
        cursorTimer = Timer(500) {
            if (isStreaming && streamingTextArea != null) {
                val text = streamingTextArea!!.text
                if (text.endsWith("▊")) {
                    streamingTextArea!!.text = text.dropLast(1)
                } else {
                    streamingTextArea!!.text = "$text▊"
                }
            }
        }
    }

    private fun buildMessageUI() {
        contentPanel.removeAll()
        codeBlocks.forEach { it.dispose() }
        codeBlocks.clear()

        if (!isAssistant) {
            // User messages: simple bubble
            buildUserMessage()
        } else {
            // Assistant messages: parse for code blocks
            buildAssistantMessage()
        }

        // Timestamp label
        val timestampLabel = JBLabel(timestamp.format(TIME_FORMATTER)).apply {
            foreground = TIMESTAMP_FG
            font = JBUI.Fonts.smallFont()
            border = JBUI.Borders.empty(JBUI.scale(2), JBUI.scale(4), 0, 0)
            alignmentX = if (isAssistant) Component.LEFT_ALIGNMENT else Component.RIGHT_ALIGNMENT
        }

        // Container for content + timestamp
        val messageContainer = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            add(contentPanel)
            add(timestampLabel)
        }

        // Alignment wrapper
        val alignmentPanel = JPanel(GridBagLayout()).apply {
            isOpaque = false
            val gbc = GridBagConstraints().apply {
                gridx = 0
                gridy = 0
                weightx = 1.0
                fill = GridBagConstraints.HORIZONTAL
                anchor = if (isAssistant) GridBagConstraints.WEST else GridBagConstraints.EAST
            }
            add(messageContainer, gbc)
        }

        add(alignmentPanel, BorderLayout.CENTER)
    }

    private fun buildUserMessage() {
        val bubblePanel = RoundedBubblePanel(JBUI.scale(12)).apply {
            layout = BorderLayout()
            background = USER_BG
            border = JBUI.Borders.empty(JBUI.scale(10), JBUI.scale(12))
            alignmentX = Component.RIGHT_ALIGNMENT
        }

        simpleTextArea = JTextArea(rawContent).apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
            background = USER_BG
            foreground = USER_FG
            font = JBUI.Fonts.label()
            border = JBUI.Borders.empty()
            isOpaque = false
        }

        bubblePanel.add(simpleTextArea, BorderLayout.CENTER)

        // Wrap to limit width
        val wrapperPanel = JPanel(BorderLayout()).apply {
            isOpaque = false
            add(bubblePanel, BorderLayout.CENTER)
            alignmentX = Component.RIGHT_ALIGNMENT
            maximumSize = Dimension(Int.MAX_VALUE, Int.MAX_VALUE)
        }

        contentPanel.add(wrapperPanel)
    }

    private fun buildAssistantMessage() {
        val segments = MarkdownParser.parse(rawContent)

        if (segments.isEmpty() || (segments.size == 1 && segments[0] is MarkdownParser.Segment.Text)) {
            // Simple text message without code blocks
            buildSimpleAssistantMessage(rawContent)
            return
        }

        // Mixed content: text and code blocks
        for (segment in segments) {
            when (segment) {
                is MarkdownParser.Segment.Text -> {
                    val textPanel = createTextSegment(segment.content)
                    contentPanel.add(textPanel)
                    contentPanel.add(Box.createVerticalStrut(JBUI.scale(4)))
                }
                is MarkdownParser.Segment.CodeBlock -> {
                    val codeComponent = CodeBlockComponent(project, segment.code, segment.language)
                    codeBlocks.add(codeComponent)
                    codeComponent.alignmentX = Component.LEFT_ALIGNMENT
                    contentPanel.add(codeComponent)
                    contentPanel.add(Box.createVerticalStrut(JBUI.scale(4)))
                }
            }
        }
    }

    private fun buildSimpleAssistantMessage(text: String) {
        val bubblePanel = RoundedBubblePanel(JBUI.scale(12)).apply {
            layout = BorderLayout()
            background = ASSISTANT_BG
            border = JBUI.Borders.empty(JBUI.scale(10), JBUI.scale(12))
            alignmentX = Component.LEFT_ALIGNMENT
        }

        simpleTextArea = JTextArea(text).apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
            background = ASSISTANT_BG
            foreground = ASSISTANT_FG
            font = JBUI.Fonts.label()
            border = JBUI.Borders.empty()
            isOpaque = false
        }
        streamingTextArea = simpleTextArea

        bubblePanel.add(simpleTextArea, BorderLayout.CENTER)

        val wrapperPanel = JPanel(BorderLayout()).apply {
            isOpaque = false
            add(bubblePanel, BorderLayout.CENTER)
            alignmentX = Component.LEFT_ALIGNMENT
        }

        contentPanel.add(wrapperPanel)
    }

    private fun createTextSegment(text: String): JPanel {
        val bubblePanel = RoundedBubblePanel(JBUI.scale(12)).apply {
            layout = BorderLayout()
            background = ASSISTANT_BG
            border = JBUI.Borders.empty(JBUI.scale(10), JBUI.scale(12))
        }

        val textArea = JTextArea(text).apply {
            isEditable = false
            lineWrap = true
            wrapStyleWord = true
            background = ASSISTANT_BG
            foreground = ASSISTANT_FG
            font = JBUI.Fonts.label()
            border = JBUI.Borders.empty()
            isOpaque = false
        }
        streamingTextArea = textArea

        bubblePanel.add(textArea, BorderLayout.CENTER)

        return JPanel(BorderLayout()).apply {
            isOpaque = false
            add(bubblePanel, BorderLayout.CENTER)
            alignmentX = Component.LEFT_ALIGNMENT
        }
    }

    override fun getPreferredSize(): Dimension {
        val pref = super.getPreferredSize()
        val parent = parent
        return if (parent != null && parent.width > 0) {
            val maxWidth = (parent.width * 0.85).toInt()  // 85% for code blocks
            Dimension(minOf(pref.width, maxWidth), pref.height)
        } else {
            pref
        }
    }

    override fun getMaximumSize(): Dimension {
        val pref = preferredSize
        return Dimension(Int.MAX_VALUE, pref.height)
    }

    fun startStreaming() {
        isStreaming = true
        cursorTimer.start()
    }

    fun stopStreaming() {
        isStreaming = false
        cursorTimer.stop()
        // Remove cursor if present
        streamingTextArea?.let { textArea ->
            val text = textArea.text
            if (text.endsWith("▊")) {
                textArea.text = text.dropLast(1)
            }
        }
        // Rebuild the message UI to properly parse and render code blocks
        rebuildContent()
    }

    fun appendText(text: String) {
        rawContent += text

        // During streaming, just append to the simple text view
        // Code blocks will be rendered when streaming stops
        if (simpleTextArea != null) {
            val currentText = simpleTextArea!!.text
            val cleanText = if (currentText.endsWith("▊")) {
                currentText.dropLast(1)
            } else {
                currentText
            }
            simpleTextArea!!.text = cleanText + text
        } else {
            // If we haven't built the UI yet, build a simple streaming view
            buildSimpleAssistantMessage(rawContent)
        }

        revalidate()
        repaint()
    }

    /**
     * Rebuild the content panel after streaming is complete.
     * This properly parses code blocks and renders them with syntax highlighting.
     */
    private fun rebuildContent() {
        if (!isAssistant) return

        // Remove all current content
        removeAll()
        contentPanel.removeAll()
        codeBlocks.forEach { it.dispose() }
        codeBlocks.clear()
        simpleTextArea = null
        streamingTextArea = null

        // Rebuild with proper parsing
        buildMessageUI()
        revalidate()
        repaint()
    }

    override fun dispose() {
        cursorTimer.stop()
        codeBlocks.forEach { it.dispose() }
        codeBlocks.clear()
    }
}
