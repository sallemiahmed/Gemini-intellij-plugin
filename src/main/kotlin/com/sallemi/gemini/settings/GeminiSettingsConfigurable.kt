package com.sallemi.gemini.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.intellij.util.ui.JBUI
import com.sallemi.gemini.api.GeminiModels
import com.sallemi.gemini.api.GeminiModelsListFetcher
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import javax.swing.*
import kotlin.math.roundToInt

/**
 * Model item for the combo box - can be either from enum or from API
 */
data class ModelItem(
    val id: String,
    val displayName: String
) {
    override fun toString(): String = displayName
}

/**
 * Modern settings UI with auto-loading models from Gemini API
 */
class GeminiSettingsConfigurable : Configurable {

    private val apiKeyField = JBPasswordField()
    private val modelComboBox = ComboBox<ModelItem>()
    private val temperatureSlider = JSlider(0, 100, 40)
    private val maxTokensField = JBTextField()
    private val streamingToggle = JBCheckBox("Enable streaming responses")
    private val modelLoadingLabel = JBLabel("")

    private var loadedModels: List<ModelItem> = emptyList()
    private var isLoadingModels = false

    override fun getDisplayName(): String = "Gemini Assistant"

    override fun createComponent(): JComponent {
        // Setup model combo box renderer
        modelComboBox.renderer = SimpleListCellRenderer.create("") { it?.displayName ?: "" }

        // Add focus listener to API key field to auto-load models when user enters API key
        apiKeyField.addFocusListener(object : FocusAdapter() {
            override fun focusLost(e: FocusEvent) {
                val apiKey = String(apiKeyField.password).trim()
                if (apiKey.isNotBlank() && loadedModels.isEmpty()) {
                    loadModelsFromApi(apiKey)
                }
            }
        })

        // Populate with default models initially
        populateDefaultModels()

        return panel {
            group("API Configuration") {
                row("API Key:") {
                    cell(apiKeyField)
                        .columns(COLUMNS_LARGE)
                        .resizableColumn()
                        .comment("Get your API key from <a href='https://makersuite.google.com/app/apikey'>Google AI Studio</a>")
                }
            }

            group("Model Settings") {
                row("Model:") {
                    cell(modelComboBox)
                        .resizableColumn()
                        .align(AlignX.FILL)
                        .comment("Models are loaded automatically from Gemini API")
                }
                row {
                    cell(modelLoadingLabel).applyToComponent {
                        foreground = JBUI.CurrentTheme.ContextHelp.FOREGROUND
                        font = JBUI.Fonts.smallFont()
                    }
                }

                row {
                    label("Temperature:")
                    cell(temperatureSlider).applyToComponent {
                        majorTickSpacing = 20
                        minorTickSpacing = 10
                        paintTicks = true
                        paintLabels = true
                        preferredSize = java.awt.Dimension(200, 50)
                    }
                    label("Max Tokens:")
                    cell(maxTokensField)
                        .columns(COLUMNS_SHORT)
                }
            }

            group("Response Options") {
                row {
                    cell(streamingToggle)
                        .comment("Enable real-time streaming of responses (recommended)")
                }
            }

            group("About") {
                row {
                    label("Gemini Assistant Pro v1.0.0")
                }
                row {
                    comment("An AI-powered coding assistant for IntelliJ IDEA")
                }
                row {
                    comment("© 2024 Sallemi Ahmed - sallemi.ahmed@gmail.com")
                }
            }
        }
    }

    /**
     * Populate combo box with default enum models
     */
    private fun populateDefaultModels() {
        modelComboBox.removeAllItems()
        GeminiModels.values().forEach { model ->
            modelComboBox.addItem(ModelItem(model.id, model.displayName))
        }
        loadedModels = GeminiModels.values().map { ModelItem(it.id, it.displayName) }
    }

    /**
     * Load models from Gemini API in background
     */
    private fun loadModelsFromApi(apiKey: String) {
        if (isLoadingModels || apiKey.isBlank()) return

        isLoadingModels = true
        modelLoadingLabel.text = "Loading models from API..."

        ProgressManager.getInstance().run(object : Task.Backgroundable(null, "Loading Gemini Models...", false) {
            var fetchedModels: List<GeminiModelsListFetcher.ModelInfo>? = null
            var error: String? = null

            override fun run(indicator: ProgressIndicator) {
                indicator.text = "Fetching models from Gemini API..."
                indicator.isIndeterminate = true

                try {
                    val fetcher = GeminiModelsListFetcher(apiKey)
                    fetchedModels = fetcher.listModels()
                } catch (e: Exception) {
                    error = e.message ?: "Unknown error"
                }
            }

            override fun onSuccess() {
                isLoadingModels = false

                if (error != null) {
                    modelLoadingLabel.text = "Failed to load models: $error"
                    return
                }

                val models = fetchedModels
                if (models.isNullOrEmpty()) {
                    modelLoadingLabel.text = "No models found. Check your API key."
                    return
                }

                // Filter to only Gemini chat models (support generateContent and are gemini models)
                val supportedModels = models
                    .filter { model ->
                        model.supportedMethods.contains("generateContent") &&
                        model.name.startsWith("gemini") &&
                        !model.name.contains("embedding") &&
                        !model.name.contains("aqa") &&
                        !model.name.contains("imagen") &&
                        !model.name.contains("learnlm")
                    }
                    .sortedBy { it.displayName }
                    .map { ModelItem(it.name, it.displayName) }

                if (supportedModels.isEmpty()) {
                    modelLoadingLabel.text = "No compatible models found."
                    return
                }

                // Save current selection
                val currentSelection = (modelComboBox.selectedItem as? ModelItem)?.id
                    ?: GeminiSettingsState.getInstance().state.model

                // Update combo box
                ApplicationManager.getApplication().invokeLater {
                    modelComboBox.removeAllItems()
                    supportedModels.forEach { modelComboBox.addItem(it) }
                    loadedModels = supportedModels

                    // Restore selection or select best default
                    val toSelect = supportedModels.find { it.id == currentSelection }
                        ?: supportedModels.find { it.id.contains("flash") && it.id.contains("1.5") }
                        ?: supportedModels.find { it.id.contains("flash") }
                        ?: supportedModels.firstOrNull()

                    if (toSelect != null) {
                        modelComboBox.selectedItem = toSelect
                    }

                    modelLoadingLabel.text = "Loaded ${supportedModels.size} chat models"
                }
            }

            override fun onThrowable(error: Throwable) {
                isLoadingModels = false
                modelLoadingLabel.text = "Error loading models"
            }
        })
    }

    override fun isModified(): Boolean {
        val state = GeminiSettingsState.getInstance().state
        val currentModel = (modelComboBox.selectedItem as? ModelItem)?.id ?: state.model
        return state.apiKey.orEmpty() != String(apiKeyField.password) ||
                state.model != currentModel ||
                kotlin.math.abs(state.temperature - sliderTemperature()) > 0.001 ||
                state.maxTokens.toString() != maxTokensField.text.trim() ||
                state.streaming != streamingToggle.isSelected
    }

    override fun apply() {
        val settings = GeminiSettingsState.getInstance()
        settings.update(settings.state.copy(
            apiKey = String(apiKeyField.password).trim(),
            model = (modelComboBox.selectedItem as? ModelItem)?.id ?: settings.state.model,
            temperature = sliderTemperature(),
            maxTokens = maxTokensField.text.toIntOrNull() ?: settings.state.maxTokens,
            streaming = streamingToggle.isSelected
        ))
    }

    override fun reset() {
        val state = GeminiSettingsState.getInstance().state
        apiKeyField.text = state.apiKey.orEmpty()
        temperatureSlider.value = (state.temperature * 100).roundToInt().coerceIn(0, 100)
        maxTokensField.text = state.maxTokens.toString()
        streamingToggle.isSelected = state.streaming

        // Load models from API if we have an API key
        val apiKey = state.apiKey.orEmpty()
        if (apiKey.isNotBlank()) {
            loadModelsFromApi(apiKey)
        }

        // Set current model selection
        val modelToSelect = loadedModels.find { it.id == state.model }
            ?: ModelItem(state.model, state.model)

        // Ensure the model exists in combo box
        var found = false
        for (i in 0 until modelComboBox.itemCount) {
            if ((modelComboBox.getItemAt(i) as? ModelItem)?.id == state.model) {
                modelComboBox.selectedIndex = i
                found = true
                break
            }
        }
        if (!found && loadedModels.isNotEmpty()) {
            modelComboBox.selectedIndex = 0
        }
    }

    private fun sliderTemperature(): Double = temperatureSlider.value / 100.0
}
