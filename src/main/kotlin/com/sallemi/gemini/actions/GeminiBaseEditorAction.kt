package com.sallemi.gemini.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.IconLoader
import com.sallemi.gemini.services.GeminiIntent
import com.sallemi.gemini.services.GeminiService

abstract class GeminiBaseEditorAction(
    text: String,
    description: String
) : AnAction(text, description, IconLoader.getIcon("/icons/gemini.svg", GeminiBaseEditorAction::class.java)),
    DumbAware {

    protected abstract val intent: GeminiIntent

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isVisible = true
        e.presentation.isEnabled = editor != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val document = editor.document
        val selectionModel = editor.selectionModel
        val selectedText = selectionModel.selectedText
        val content = selectedText?.takeIf { it.isNotBlank() } ?: document.text
        GeminiService.getInstance(project).sendIntent(intent, content, extraContext(e))
    }

    protected open fun extraContext(event: AnActionEvent): String? = null
}
