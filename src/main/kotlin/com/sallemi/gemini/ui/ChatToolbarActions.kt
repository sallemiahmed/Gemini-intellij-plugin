package com.sallemi.gemini.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAware
import com.sallemi.gemini.services.GeminiService
import com.sallemi.gemini.settings.GeminiSettingsConfigurable

/**
 * Toolbar action to create a new chat (clears conversation)
 */
class NewChatAction(private val chatPanel: GeminiChatPanel) : AnAction(
    "New Chat",
    "Start a new conversation",
    AllIcons.Actions.Refresh
), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        chatPanel.clearChat()
    }
}

/**
 * Toolbar action to clear the chat display
 */
class ClearChatAction(private val chatPanel: GeminiChatPanel) : AnAction(
    "Clear Chat",
    "Clear all messages from the chat",
    AllIcons.Actions.GC
), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        chatPanel.clearChat()
    }
}

/**
 * Toolbar action to stop ongoing generation
 */
class StopGenerationAction(private val service: GeminiService) : AnAction(
    "Stop Generation",
    "Stop the current AI response generation",
    AllIcons.Actions.Suspend
), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        service.stopGeneration()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = service.isGenerating()
    }
}

/**
 * Toolbar action to open settings
 */
class OpenSettingsAction : AnAction(
    "Settings",
    "Open Gemini Assistant settings",
    AllIcons.General.Settings
), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        ShowSettingsUtil.getInstance().showSettingsDialog(
            project,
            GeminiSettingsConfigurable::class.java
        )
    }
}
