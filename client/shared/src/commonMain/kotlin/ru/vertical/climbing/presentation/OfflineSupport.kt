package ru.vertical.climbing.presentation

import ru.vertical.climbing.domain.util.ErrorCode

/**
 * Общая логика offline-fallback (NFR-001, Итерация 9):
 * при сетевой ошибке показываем последние/кэшированные данные с баннером.
 */
data class OfflineFallback<T>(
    val content: Async.Content<T>,
    val isOffline: Boolean = true,
)

fun <T> offlineFallbackOrNull(code: ErrorCode, cached: T?): OfflineFallback<T>? =
    if (code == ErrorCode.NETWORK && cached != null) {
        OfflineFallback(content = Async.Content(cached))
    } else {
        null
    }

fun <T> offlineListFallbackOrNull(code: ErrorCode, cached: List<T>): OfflineFallback<List<T>>? =
    offlineFallbackOrNull(code, cached.takeIf { it.isNotEmpty() })
