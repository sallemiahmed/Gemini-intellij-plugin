package com.sallemi.gemini.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ui.Messages
import com.sallemi.gemini.services.ConvertLanguageIntent
import com.sallemi.gemini.services.GeminiIntent

class ConvertLanguageAction : GeminiBaseEditorAction(
    "Convert Code to Another Language",
    "Convert selected code to another language using Gemini"
) {
    override val intent: GeminiIntent = ConvertLanguageIntent

    override fun extraContext(event: AnActionEvent): String? {
        val project = event.project ?: return null
        return Messages.showInputDialog(
            project,
            "Target language (optional):",
            "Gemini Convert Code",
            null
        )?.let { "Target language: $it" }
    }
}
