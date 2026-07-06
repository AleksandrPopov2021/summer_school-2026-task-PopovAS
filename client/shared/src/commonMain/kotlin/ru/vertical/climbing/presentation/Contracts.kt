package ru.vertical.climbing.presentation

/** Маркер иммутабельного состояния экрана (MVI). */
interface UiState

/** Маркер намерения пользователя (MVI). */
interface UiEvent

/** Маркер одноразового эффекта (навигация, snackbar). */
interface UiEffect

/**
 * Обобщённое состояние загрузки контента для переиспользуемых
 * Loading / Error / Empty composables.
 */
sealed interface Async<out T> {
    data object Idle : Async<Nothing>
    data object Loading : Async<Nothing>
    data class Content<T>(val data: T) : Async<T>
    data object Empty : Async<Nothing>
    data class Error(val error: ru.vertical.climbing.domain.util.AppError) : Async<Nothing>
}
