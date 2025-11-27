package com.sallemi.gemini.ui

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.sallemi.gemini.services.GeminiService
import java.awt.BorderLayout
import javax.swing.JPanel

/**
 * Factory for creating the Gemini tool window with native IntelliJ UI
 */
class GeminiToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val chatPanel = GeminiChatPanel(project)
        val service = GeminiService.getInstance(project)
        service.attachPanel(chatPanel)

        // Create toolbar
        val toolbar = createToolbar(project, chatPanel, service)

        // Main panel with toolbar and chat
        val mainPanel = JPanel(BorderLayout()).apply {
            add(toolbar.component, BorderLayout.NORTH)
            add(chatPanel.component, BorderLayout.CENTER)
        }

        val content = ContentFactory.getInstance()
            .createContent(mainPanel, "", false)
        toolWindow.contentManager.addContent(content)
    }

    private fun createToolbar(
        project: Project,
        chatPanel: GeminiChatPanel,
        service: GeminiService
    ): ActionToolbar {
        val actionGroup = DefaultActionGroup().apply {
            add(NewChatAction(chatPanel))
            add(ClearChatAction(chatPanel))
            addSeparator()
            add(StopGenerationAction(service))
            addSeparator()
            add(OpenSettingsAction())
        }

        return ActionManager.getInstance()
            .createActionToolbar("GeminiToolbar", actionGroup, true).apply {
                targetComponent = chatPanel.component
            }
    }
}
