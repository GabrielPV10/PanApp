package com.example.panapp.ui

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/** Estado de carga de una pantalla con datos asíncronos. */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data object Empty : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

/**
 * Convierte un flujo de lista en un flujo de [UiState]: emite [UiState.Loading]
 * al inicio, [UiState.Empty] si la lista llega vacía y [UiState.Success] con los
 * datos en caso contrario; cualquier excepción se mapea a [UiState.Error].
 */
fun <T> Flow<List<T>>.asUiState(): Flow<UiState<List<T>>> =
    asUiState { it.isEmpty() }

/**
 * Variante para cualquier tipo: usa [isEmpty] para decidir cuándo emitir
 * [UiState.Empty] en lugar de [UiState.Success].
 */
fun <T> Flow<T>.asUiState(isEmpty: (T) -> Boolean): Flow<UiState<T>> =
    map { valor ->
        if (isEmpty(valor)) UiState.Empty else UiState.Success(valor)
    }
        .onStart { emit(UiState.Loading) }
        .catch { emit(UiState.Error(it.message ?: "Error desconocido")) }
