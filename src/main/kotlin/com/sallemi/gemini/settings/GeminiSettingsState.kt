package com.sallemi.gemini.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.sallemi.gemini.api.GeminiModels

@Service(Service.Level.APP)
@State(name = "GeminiSettingsState", storages = [Storage("GeminiSettings.xml")])
class GeminiSettingsState : PersistentStateComponent<GeminiSettingsState.State> {

    data class State(
        var apiKey: String? = "",
        var model: String = GeminiModels.default().id,
        var temperature: Double = 0.4,
        var maxTokens: Int = 2048,
        var streaming: Boolean = true
    )

    private var myState: State = State()

    override fun getState(): State = myState

    override fun loadState(state: State) {
        this.myState = state
    }

    fun update(state: State) {
        this.myState = state
    }

    companion object {
        fun getInstance(): GeminiSettingsState =
            ApplicationManager.getApplication().getService(GeminiSettingsState::class.java)
    }
}
