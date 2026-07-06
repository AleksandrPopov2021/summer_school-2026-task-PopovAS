package ru.vertical.climbing.data.repository

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.serialization.json.Json
import ru.vertical.climbing.data.local.LocalCache
import ru.vertical.climbing.data.mapper.toDomain
import ru.vertical.climbing.data.remote.apiCall
import ru.vertical.climbing.data.remote.dto.SystemConfigDto
import ru.vertical.climbing.data.remote.readResult
import ru.vertical.climbing.domain.model.SystemConfig
import ru.vertical.climbing.domain.repository.ConfigRepository
import ru.vertical.climbing.domain.util.AppResult
import ru.vertical.climbing.domain.util.map
import ru.vertical.climbing.domain.util.onSuccess

class ConfigRepositoryImpl(
    private val client: HttpClient,
    private val cache: LocalCache,
    private val json: Json,
) : ConfigRepository {

    override suspend fun getConfig(forceRefresh: Boolean): AppResult<SystemConfig> {
        if (!forceRefresh) {
            cache.readConfig()?.let { return AppResult.Success(it.toDomain()) }
        }
        return apiCall {
            client.get("config")
                .readResult<SystemConfigDto>(json)
                .onSuccess { cache.saveConfig(it) }
                .map { it.toDomain() }
        }
    }

    override suspend fun cachedConfig(): SystemConfig? = cache.readConfig()?.toDomain()
}
