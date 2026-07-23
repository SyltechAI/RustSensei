package com.sylvester.rustsensei.llm

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Process-wide holder for the current model-download progress.
 *
 * [ModelDownloadService] owns the download coroutine and pushes state here;
 * ModelViewModel observes it. This decouples the long-running ~1.2 GB download
 * from any ViewModel/Activity lifecycle, so progress survives configuration
 * changes and screen navigation without an app-managed wake lock.
 */
@Singleton
class DownloadStateHolder @Inject constructor() {

    private val _state = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val state: StateFlow<DownloadState> = _state.asStateFlow()

    fun update(state: DownloadState) {
        _state.value = state
    }
}
