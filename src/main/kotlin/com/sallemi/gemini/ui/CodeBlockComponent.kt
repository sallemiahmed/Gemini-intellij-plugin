package com.sallemi.gemini.ui

import com.intellij.ide.highlighter.HighlighterFactory
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.highlighter.EditorHighlighter
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import java.awt.*
import java.awt.datatransfer.StringSelection
import javax.swing.BorderFactory
import javax.swing.JPanel
import javax.swing.Timer

/**
 * A component that displays a syntax-highlighted code block using IntelliJ's editor.
 *
 * Features:
 * - Syntax highlighting based on language
 * - Copy button
 * - Language label
 * - Themed to match IDE colors
 */
class CodeBlockComponent(
    private val project: Project?,
    private val code: String,
    private val language: String?
) : JPanel() {

    private var editor: EditorEx? = null

    companion object {
        private val HEADER_BG = JBColor(0xF6F8FA, 0x2D2D2D)
        private val CODE_BG = JBColor(0xFFFFFF, 0x1E1E1E)
        private val BORDER_COLOR = JBColor(0xD0D7DE, 0x3D3D3D)
        private val LANGUAGE_FG = JBColor(0x656D76, 0x8B949E)
    }

    init {
        layout = BorderLayout()
        isOpaque = false
        border = JBUI.Borders.empty(JBUI.scale(4), 0)

        // Main container with border
        val container = JPanel(BorderLayout()).apply {
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1, true),
                JBUI.Borders.empty()
            )
            background = CODE_BG
        }

        // Header with language and copy button
        val header = createHeader()
        container.add(header, BorderLayout.NORTH)

        // Code editor
        val editorComponent = createEditor()
        container.add(editorComponent, BorderLayout.CENTER)

        add(container, BorderLayout.CENTER)
    }

    private fun createHeader(): JPanel {
        val header = JPanel(BorderLayout()).apply {
            background = HEADER_BG
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                JBUI.Borders.empty(JBUI.scale(4), JBUI.scale(12))
            )
        }

        // Language label
        val displayLanguage = language?.let { MarkdownParser.normalizeLanguage(it) } ?: "Code"
        val languageLabel = JBLabel(displayLanguage).apply {
            font = JBUI.Fonts.smallFont()
            foreground = LANGUAGE_FG
        }
        header.add(languageLabel, BorderLayout.WEST)

        // Copy button
        val copyButton = createCopyButton()
        header.add(copyButton, BorderLayout.EAST)

        return header
    }

    private fun createCopyButton(): JPanel {
        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 0, 0)).apply {
            isOpaque = false
        }

        val copyLabel = JBLabel("Copy").apply {
            font = JBUI.Fonts.smallFont()
            foreground = JBColor(0x0969DA, 0x58A6FF)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            border = JBUI.Borders.empty(JBUI.scale(2), JBUI.scale(8))

            addMouseListener(object : java.awt.event.MouseAdapter() {
                override fun mouseClicked(e: java.awt.event.MouseEvent) {
                    copyToClipboard()
                    // Show feedback
                    val originalText = text
                    text = "Copied!"
                    foreground = JBColor(0x1A7F37, 0x3FB950)
                    Timer(1500) {
                        text = originalText
                        foreground = JBColor(0x0969DA, 0x58A6FF)
                    }.apply {
                        isRepeats = false
                        start()
                    }
                }

                override fun mouseEntered(e: java.awt.event.MouseEvent) {
                    foreground = JBColor(0x0550AE, 0x79C0FF)
                }

                override fun mouseExited(e: java.awt.event.MouseEvent) {
                    foreground = JBColor(0x0969DA, 0x58A6FF)
                }
            })
        }

        buttonPanel.add(copyLabel)
        return buttonPanel
    }

    private fun copyToClipboard() {
        val selection = StringSelection(code)
        CopyPasteManager.getInstance().setContents(selection)
    }

    private fun createEditor(): Component {
        val editorFactory = EditorFactory.getInstance()
        val document = editorFactory.createDocument(code)

        editor = editorFactory.createViewer(document, project) as EditorEx

        editor?.apply {
            // Configure editor settings
            settings.apply {
                isLineNumbersShown = true
                isWhitespacesShown = false
                isFoldingOutlineShown = false
                isRightMarginShown = false
                additionalLinesCount = 0
                additionalColumnsCount = 0
                isAdditionalPageAtBottom = false
                isCaretRowShown = false
                isUseSoftWraps = true
            }

            // Set background color
            backgroundColor = CODE_BG

            // Apply syntax highlighting
            highlighter = createHighlighter()

            // Remove gutter icons but keep line numbers
            gutterComponentEx.apply {
                setInitialIconAreaWidth(0)
            }

            // Calculate preferred height based on content
            val lineCount = document.lineCount
            val lineHeight = lineHeight
            val headerHeight = JBUI.scale(30) // Approximate header height
            val minHeight = JBUI.scale(60)
            val maxHeight = JBUI.scale(400)
            val calculatedHeight = (lineCount * lineHeight) + JBUI.scale(16) // Add padding

            component.preferredSize = Dimension(
                component.preferredSize.width,
                calculatedHeight.coerceIn(minHeight, maxHeight)
            )
        }

        return editor?.component ?: JPanel()
    }

    private fun createHighlighter(): EditorHighlighter {
        val scheme = EditorColorsManager.getInstance().globalScheme

        // Try to find appropriate file type for the language
        val fileType = language?.let { lang ->
            findFileTypeForLanguage(lang)
        } ?: PlainTextFileType.INSTANCE

        return HighlighterFactory.createHighlighter(fileType, scheme, project)
    }

    private fun findFileTypeForLanguage(language: String): com.intellij.openapi.fileTypes.FileType {
        val fileTypeManager = FileTypeManager.getInstance()
        val normalizedLang = language.lowercase()

        // Map language names to file extensions
        val extension = when (normalizedLang) {
            "kotlin", "kt" -> "kt"
            "java" -> "java"
            "javascript", "js" -> "js"
            "typescript", "ts" -> "ts"
            "python", "py" -> "py"
            "ruby", "rb" -> "rb"
            "go", "golang" -> "go"
            "rust", "rs" -> "rs"
            "c" -> "c"
            "cpp", "c++" -> "cpp"
            "csharp", "c#", "cs" -> "cs"
            "swift" -> "swift"
            "scala" -> "scala"
            "groovy" -> "groovy"
            "php" -> "php"
            "html" -> "html"
            "css" -> "css"
            "scss", "sass" -> "scss"
            "xml" -> "xml"
            "json" -> "json"
            "yaml", "yml" -> "yaml"
            "sql" -> "sql"
            "shell", "bash", "sh", "zsh" -> "sh"
            "powershell", "ps1" -> "ps1"
            "markdown", "md" -> "md"
            "dockerfile" -> "Dockerfile"
            else -> normalizedLang
        }

        return fileTypeManager.getFileTypeByExtension(extension)
    }

    /**
     * Cleanup editor resources when component is no longer needed
     */
    fun dispose() {
        editor?.let { ed ->
            EditorFactory.getInstance().releaseEditor(ed)
        }
        editor = null
    }

    /**
     * Update the code content (for streaming)
     */
    fun updateCode(newCode: String) {
        editor?.let { ed ->
            val document = ed.document
            com.intellij.openapi.application.ApplicationManager.getApplication().runWriteAction {
                document.setText(newCode)
            }
        }
    }
}
