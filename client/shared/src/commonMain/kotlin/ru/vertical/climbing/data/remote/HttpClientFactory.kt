package ru.vertical.climbing.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import ru.vertical.climbing.data.local.TokenStore
import ru.vertical.climbing.data.remote.mock.MockApi

/** Сборка [HttpClient]: mock-движок для разработки или реальный движок платформы. */
object HttpClientFactory {

    fun create(
        config: ApiConfig,
        tokenStore: TokenStore,
        json: Json = appJson,
    ): HttpClient {
        val engine = if (config.useMock) MockApi.createEngine(json) else createPlatformEngine()

        return HttpClient(engine) {
            expectSuccess = false

            install(ContentNegotiation) {
                json(json)
            }

            install(DefaultRequest) {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Accept, ContentType.Application.Json.toString())
                url(config.baseUrl.ensureTrailingSlash())
            }

            install(Auth) {
                bearer {
                    loadTokens {
                        tokenStore.read()?.let { BearerTokens(it, "") }
                    }
                    sendWithoutRequest { true }
                }
            }

            if (config.enableLogging) {
                install(Logging) {
                    level = LogLevel.INFO
                }
            }
        }
    }

    private fun String.ensureTrailingSlash(): String = if (endsWith("/")) this else "$this/"
}
