package com.sallemi.gemini.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.sallemi.gemini.settings.GeminiSettingsConfigurable
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*

/**
 * Beautiful onboarding panel displayed when no API key is configured
 */
class EmptyStatePanel(private val project: Project) : JPanel(BorderLayout()) {

    init {
        background = UIUtil.getPanelBackground()
        border = JBUI.Borders.empty(40)

        val centerPanel = JPanel(GridBagLayout()).apply {
            background = UIUtil.getPanelBackground()
        }

        val gbc = GridBagConstraints().apply {
            gridx = 0
            gridy = GridBagConstraints.RELATIVE
            anchor = GridBagConstraints.CENTER
            insets = JBUI.insets(10, 0)
        }

        // Large icon
        val iconLabel = JBLabel(AllIcons.General.BalloonInformation).apply {
            icon = AllIcons.General.BalloonInformation
            // Scale up the icon
            preferredSize = Dimension(64, 64)
        }
        centerPanel.add(iconLabel, gbc)

        // Title
        val titleLabel = JBLabel("Welcome to Gemini Assistant Pro").apply {
            font = JBUI.Fonts.label(24f)
            foreground = UIUtil.getLabelForeground()
        }
        centerPanel.add(titleLabel, gbc)

        // Subtitle
        val subtitleLabel = JBLabel("Enter your Gemini API key to begin").apply {
            font = JBUI.Fonts.label(14f)
            foreground = JBColor.GRAY
        }
        centerPanel.add(subtitleLabel, gbc)

        // Spacer
        gbc.insets = JBUI.insets(20, 0, 10, 0)
        centerPanel.add(Box.createVerticalStrut(10), gbc)

        // Setup button
        val setupButton = JButton("Setup API Key").apply {
            icon = AllIcons.General.Settings
            preferredSize = Dimension(200, 40)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            addActionListener {
                // Show first-run setup dialog
                val completed = com.sallemi.gemini.settings.FirstRunSetupDialog.showAndSave(project)
                if (completed) {
                    // Refresh the panel to show chat UI
                    val parent = this@EmptyStatePanel.parent
                    if (parent != null) {
                        parent.removeAll()
                        val newPanel = GeminiChatPanel(project)
                        com.sallemi.gemini.services.GeminiService.getInstance(project).attachPanel(newPanel)
                        parent.add(newPanel.component)
                        parent.revalidate()
                        parent.repaint()
                    }
                }
            }
        }
        gbc.insets = JBUI.insets(10, 0)
        centerPanel.add(setupButton, gbc)

        // Instructions
        gbc.insets = JBUI.insets(30, 0, 0, 0)
        val instructionsLabel = JBLabel("<html><center>" +
                "Get your API key from<br/>" +
                "<a href='https://makersuite.google.com/app/apikey'>Google AI Studio</a>" +
                "</center></html>").apply {
            font = JBUI.Fonts.smallFont()
            foreground = JBColor.GRAY
        }
        centerPanel.add(instructionsLabel, gbc)

        add(centerPanel, BorderLayout.CENTER)
    }
}
