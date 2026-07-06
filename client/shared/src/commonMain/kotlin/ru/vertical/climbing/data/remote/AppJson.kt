package ru.vertical.climbing.data.remote

import kotlinx.serialization.json.Json

/** Единая конфигурация kotlinx.serialization для сети и кэша. */
val appJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
    explicitNulls = false
}
