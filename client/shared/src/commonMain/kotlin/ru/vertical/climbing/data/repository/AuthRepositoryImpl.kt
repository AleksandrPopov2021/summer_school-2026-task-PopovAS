package ru.vertical.climbing.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.serialization.json.Json
import ru.vertical.climbing.data.local.TokenStore
import ru.vertical.climbing.data.mapper.toDomain
import ru.vertical.climbing.data.mapper.toDto
import ru.vertical.climbing.data.remote.dto.ClientDto
import ru.vertical.climbing.data.remote.dto.ClientRegistrationResponseDto
import ru.vertical.climbing.data.remote.dto.ClientUpdateRequestDto
import ru.vertical.climbing.data.remote.readResult
import ru.vertical.climbing.data.remote.apiCall
import ru.vertical.climbing.domain.model.AuthSession
import ru.vertical.climbing.domain.model.Client
import ru.vertical.climbing.domain.model.ClientRegistration
import ru.vertical.climbing.domain.repository.AuthRepository
import ru.vertical.climbing.domain.util.AppResult
import ru.vertical.climbing.domain.util.map

class AuthRepositoryImpl(
    private val client: HttpClient,
    private val tokenStore: TokenStore,
    private val json: Json,
) : AuthRepository {

    override suspend fun register(registration: ClientRegistration): AppResult<AuthSession> = apiCall {
        client.post("clients") { setBody(registration.toDto()) }
            .readResult<ClientRegistrationResponseDto>(json)
            .map { AuthSession(accessToken = it.accessToken, client = it.client.toDomain()) }
    }

    override suspend fun getCurrentClient(): AppResult<Client> = apiCall {
        client.get("clients/me")
            .readResult<ClientDto>(json)
            .map { it.toDomain() }
    }

    override suspend fun acceptRiskConsent(): AppResult<Client> = apiCall {
        client.patch("clients/me") { setBody(ClientUpdateRequestDto(riskConsentAccepted = true)) }
            .readResult<ClientDto>(json)
            .map { it.toDomain() }
    }

    override suspend fun saveToken(token: String) = tokenStore.write(token)
    override suspend fun readToken(): String? = tokenStore.read()
    override suspend fun clearToken() = tokenStore.clear()
    override suspend fun hasToken(): Boolean = tokenStore.has()
}
