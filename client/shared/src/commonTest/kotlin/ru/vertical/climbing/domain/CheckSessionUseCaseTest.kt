package ru.vertical.climbing.domain

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import ru.vertical.climbing.domain.usecase.CheckSessionUseCase
import ru.vertical.climbing.domain.usecase.SessionState
import ru.vertical.climbing.domain.util.AppResult
import ru.vertical.climbing.domain.util.ErrorCode

class CheckSessionUseCaseTest {

    @Test
    fun no_token_routes_to_registration() = runTest {
        val useCase = CheckSessionUseCase(
            authRepository = FakeAuthRepository(tokenPresent = false),
            configRepository = FakeConfigRepository(),
        )
        val result = useCase()
        assertIs<AppResult.Success<SessionState>>(result)
        assertEquals(SessionState.Unauthenticated, result.value)
    }

    @Test
    fun valid_token_routes_to_schedule() = runTest {
        val useCase = CheckSessionUseCase(
            authRepository = FakeAuthRepository(
                tokenPresent = true,
                currentClientResult = AppResult.Success(sampleClient),
            ),
            configRepository = FakeConfigRepository(),
        )
        val result = useCase()
        assertIs<AppResult.Success<SessionState>>(result)
        val state = result.value
        assertIs<SessionState.Authenticated>(state)
        assertEquals(sampleClient, state.client)
    }

    @Test
    fun expired_token_clears_and_routes_to_registration() = runTest {
        val auth = FakeAuthRepository(
            tokenPresent = true,
            currentClientResult = failure(ErrorCode.UNAUTHORIZED),
        )
        val useCase = CheckSessionUseCase(auth, FakeConfigRepository())
        val result = useCase()
        assertIs<AppResult.Success<SessionState>>(result)
        assertEquals(SessionState.Unauthenticated, result.value)
        assertTrue(auth.clearTokenCalled)
    }

    @Test
    fun offline_with_token_routes_to_offline_schedule() = runTest {
        val useCase = CheckSessionUseCase(
            authRepository = FakeAuthRepository(
                tokenPresent = true,
                currentClientResult = failure(ErrorCode.NETWORK),
            ),
            configRepository = FakeConfigRepository(),
        )
        val result = useCase()
        assertIs<AppResult.Success<SessionState>>(result)
        assertEquals(SessionState.Offline, result.value)
    }

    @Test
    fun server_error_with_token_propagates_failure() = runTest {
        val useCase = CheckSessionUseCase(
            authRepository = FakeAuthRepository(
                tokenPresent = true,
                currentClientResult = failure(ErrorCode.SERVER_ERROR),
            ),
            configRepository = FakeConfigRepository(),
        )
        val result = useCase()
        assertIs<AppResult.Failure>(result)
        assertEquals(ErrorCode.SERVER_ERROR, result.error.code)
    }
}
