package com.sallemi.gemini.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.IconLoader
import com.sallemi.gemini.services.GeminiService

class GeminiChatAction : AnAction(
    "Gemini Chat",
    "Open Gemini chat tool window",
    IconLoader.getIcon("/icons/gemini.svg", GeminiChatAction::class.java)
), DumbAware {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        GeminiService.getInstance(project).openChat()
    }
}
