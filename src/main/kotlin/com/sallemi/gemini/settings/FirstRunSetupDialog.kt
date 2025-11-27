package com.sallemi.gemini.settings

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.util.ui.JBUI
import com.sallemi.gemini.api.GeminiModels
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*

/**
 * First-run setup dialog for API key configuration
 * Shown when no API key is configured on plugin first use
 */
class FirstRunSetupDialog(project: Project?) : DialogWrapper(project) {

    private val apiKeyField = JBPasswordField()
    private val modelComboBox = ComboBox(GeminiModels.values())

    var apiKey: String = ""
        private set

    var selectedModel: GeminiModels = GeminiModels.default()
        private set

    init {
        title = "Gemini Assistant Pro - Setup"
        modelComboBox.selectedItem = GeminiModels.default()
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridBagLayout()).apply {
            border = JBUI.Borders.empty(10)
            preferredSize = Dimension(500, 250)
        }

        val gbc = GridBagConstraints().apply {
            gridx = 0
            gridy = GridBagConstraints.RELATIVE
            anchor = GridBagConstraints.WEST
            insets = JBUI.insets(8)
            fill = GridBagConstraints.HORIZONTAL
            weightx = 1.0
        }

        // Welcome header
        val headerLabel = JBLabel("<html><b style='font-size:14pt'>Welcome to Gemini Assistant Pro!</b></html>").apply {
            border = JBUI.Borders.empty(0, 0, 10, 0)
        }
        panel.add(headerLabel, gbc)

        // Description
        val descLabel = JBLabel("<html>To get started, please enter your Google Gemini API key.<br/>" +
                "You can obtain one from <a href='https://makersuite.google.com/app/apikey'>Google AI Studio</a>.</html>").apply {
            border = JBUI.Borders.empty(0, 0, 15, 0)
        }
        panel.add(descLabel, gbc)

        // API Key label
        val apiKeyLabel = JBLabel("API Key:")
        gbc.insets = JBUI.insets(5, 8, 2, 8)
        panel.add(apiKeyLabel, gbc)

        // API Key field
        apiKeyField.preferredSize = Dimension(400, 30)
        gbc.insets = JBUI.insets(0, 8, 10, 8)
        panel.add(apiKeyField, gbc)

        // Model label
        val modelLabel = JBLabel("Model:")
        gbc.insets = JBUI.insets(5, 8, 2, 8)
        panel.add(modelLabel, gbc)

        // Model combo box
        gbc.insets = JBUI.insets(0, 8, 10, 8)
        panel.add(modelComboBox, gbc)

        // Info label
        val infoLabel = JBLabel("<html><i>You can change these settings later in:<br/>" +
                "Tools → Gemini Assistant Pro Settings</i></html>").apply {
            foreground = JBUI.CurrentTheme.ContextHelp.FOREGROUND
            border = JBUI.Borders.empty(10, 0, 0, 0)
        }
        panel.add(infoLabel, gbc)

        return panel
    }

    override fun doOKAction() {
        apiKey = String(apiKeyField.password).trim()
        selectedModel = modelComboBox.selectedItem as? GeminiModels ?: GeminiModels.default()

        if (apiKey.isBlank()) {
            JOptionPane.showMessageDialog(
                contentPanel,
                "Please enter a valid API key.",
                "API Key Required",
                JOptionPane.WARNING_MESSAGE
            )
            return
        }

        super.doOKAction()
    }

    companion object {
        /**
         * Show the first-run setup dialog and save settings if confirmed
         * @return true if setup was completed, false if cancelled
         */
        fun showAndSave(project: Project?): Boolean {
            val dialog = FirstRunSetupDialog(project)
            if (dialog.showAndGet()) {
                val settings = GeminiSettingsState.getInstance()
                settings.update(settings.state.copy(
                    apiKey = dialog.apiKey,
                    model = dialog.selectedModel.id
                ))
                return true
            }
            return false
        }
    }
}
