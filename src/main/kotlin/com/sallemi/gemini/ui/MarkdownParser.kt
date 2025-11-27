package com.sallemi.gemini.ui

/**
 * Parses markdown text to identify code blocks and regular text segments.
 * Uses string-based parsing for reliability across different line ending formats.
 */
object MarkdownParser {

    /**
     * Represents a segment of parsed markdown content
     */
    sealed class Segment {
        data class Text(val content: String) : Segment()
        data class CodeBlock(val language: String?, val code: String) : Segment()
    }

    private const val CODE_FENCE = "```"

    /**
     * Parse markdown text into segments of text and code blocks.
     */
    fun parse(text: String): List<Segment> {
        if (text.isBlank()) return emptyList()

        val segments = mutableListOf<Segment>()
        var remaining = text

        while (remaining.isNotEmpty()) {
            val codeStart = remaining.indexOf(CODE_FENCE)

            if (codeStart == -1) {
                // No more code blocks, add remaining as text
                val trimmed = remaining.trim()
                if (trimmed.isNotEmpty()) {
                    segments.add(Segment.Text(trimmed))
                }
                break
            }

            // Add any text before this code block
            if (codeStart > 0) {
                val textBefore = remaining.substring(0, codeStart).trim()
                if (textBefore.isNotEmpty()) {
                    segments.add(Segment.Text(textBefore))
                }
            }

            // Parse the code block starting after opening ```
            val afterOpening = remaining.substring(codeStart + 3)

            // Find the end of the first line to get the language
            val firstNewline = afterOpening.indexOfFirst { it == '\n' || it == '\r' }

            val language: String?
            val codeContentStart: Int

            if (firstNewline >= 0) {
                // Get language from first line (e.g., "javascript" from "```javascript")
                val langLine = afterOpening.substring(0, firstNewline).trim()
                language = langLine.takeIf { it.isNotEmpty() && it.all { c -> c.isLetterOrDigit() || c == '+' || c == '#' } }
                // Skip past the newline(s)
                codeContentStart = if (firstNewline + 1 < afterOpening.length && afterOpening[firstNewline] == '\r' && afterOpening[firstNewline + 1] == '\n') {
                    firstNewline + 2
                } else {
                    firstNewline + 1
                }
            } else {
                // No newline found, might be malformed
                language = null
                codeContentStart = 0
            }

            // Find the closing ```
            val codeContent = afterOpening.substring(codeContentStart)
            val codeEnd = codeContent.indexOf(CODE_FENCE)

            if (codeEnd == -1) {
                // No closing fence found - treat everything as a code block anyway
                val code = codeContent.trim()
                if (code.isNotEmpty()) {
                    segments.add(Segment.CodeBlock(language, code))
                }
                break
            }

            // Extract the code content
            val code = codeContent.substring(0, codeEnd).trimEnd()
            if (code.isNotEmpty() || language != null) {
                segments.add(Segment.CodeBlock(language, code))
            }

            // Move past the closing ``` and continue
            remaining = codeContent.substring(codeEnd + 3)
        }

        // If no segments were found, return text as-is
        if (segments.isEmpty() && text.isNotBlank()) {
            segments.add(Segment.Text(text))
        }

        return segments
    }

    /**
     * Check if text contains any code blocks
     */
    fun hasCodeBlocks(text: String): Boolean {
        val firstFence = text.indexOf(CODE_FENCE)
        if (firstFence == -1) return false
        val secondFence = text.indexOf(CODE_FENCE, firstFence + 3)
        return secondFence != -1
    }

    /**
     * Map common language aliases to display names
     */
    fun normalizeLanguage(language: String?): String {
        return when (language?.lowercase()) {
            "kotlin", "kt" -> "Kotlin"
            "java" -> "Java"
            "javascript", "js" -> "JavaScript"
            "typescript", "ts" -> "TypeScript"
            "python", "py" -> "Python"
            "ruby", "rb" -> "Ruby"
            "go", "golang" -> "Go"
            "rust", "rs" -> "Rust"
            "c" -> "C"
            "cpp", "c++" -> "C++"
            "csharp", "c#", "cs" -> "C#"
            "swift" -> "Swift"
            "scala" -> "Scala"
            "groovy" -> "Groovy"
            "php" -> "PHP"
            "html" -> "HTML"
            "css" -> "CSS"
            "scss", "sass" -> "SCSS"
            "xml" -> "XML"
            "json" -> "JSON"
            "yaml", "yml" -> "YAML"
            "sql" -> "SQL"
            "shell", "bash", "sh", "zsh" -> "Shell"
            "powershell", "ps1" -> "PowerShell"
            "markdown", "md" -> "Markdown"
            "dockerfile" -> "Dockerfile"
            "plaintext", "text", "txt" -> "Text"
            else -> language ?: "Code"
        }
    }
}
