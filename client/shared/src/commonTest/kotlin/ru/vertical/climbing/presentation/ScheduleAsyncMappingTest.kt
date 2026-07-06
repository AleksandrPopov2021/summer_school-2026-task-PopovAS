package ru.vertical.climbing.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import ru.vertical.climbing.domain.util.AppError
import ru.vertical.climbing.domain.util.AppResult
import ru.vertical.climbing.domain.util.ErrorCode

class ScheduleAsyncMappingTest {

    @Test
    fun empty_list_maps_to_empty_state() {
        val result: AppResult<List<String>> = AppResult.Success(emptyList())
        assertEquals(Async.Empty, result.toListAsync())
    }

    @Test
    fun non_empty_list_maps_to_content() {
        val result: AppResult<List<String>> = AppResult.Success(listOf("slot-1"))
        val async = result.toListAsync()
        assertIs<Async.Content<List<String>>>(async)
        assertEquals(listOf("slot-1"), async.data)
    }

    @Test
    fun failure_maps_to_error() {
        val result: AppResult<List<String>> = AppResult.Failure(AppError(ErrorCode.NETWORK))
        val async = result.toListAsync()
        assertIs<Async.Error>(async)
        assertEquals(ErrorCode.NETWORK, async.error.code)
    }
}
