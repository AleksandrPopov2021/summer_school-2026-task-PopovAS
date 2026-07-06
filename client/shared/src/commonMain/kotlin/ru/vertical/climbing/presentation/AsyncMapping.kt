package ru.vertical.climbing.presentation

import ru.vertical.climbing.domain.util.AppResult

/**
 * Преобразование результата загрузки списка в [Async]:
 * пустой список → [Async.Empty] (empty state, SCR-013, FR-005),
 * непустой → [Async.Content], ошибка → [Async.Error].
 */
fun <T> AppResult<List<T>>.toListAsync(): Async<List<T>> = when (this) {
    is AppResult.Success -> if (value.isEmpty()) Async.Empty else Async.Content(value)
    is AppResult.Failure -> Async.Error(error)
}
