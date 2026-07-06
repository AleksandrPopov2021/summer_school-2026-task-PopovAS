package ru.vertical.climbing.data.local

import com.russhwolf.settings.Settings

/** Хранилище JWT доступа (secure). */
class TokenStore(private val settings: Settings) {

    fun read(): String? = settings.getStringOrNull(StorageKeys.ACCESS_TOKEN)

    fun write(token: String) {
        settings.putString(StorageKeys.ACCESS_TOKEN, token)
    }

    fun clear() {
        settings.remove(StorageKeys.ACCESS_TOKEN)
    }

    fun has(): Boolean = settings.hasKey(StorageKeys.ACCESS_TOKEN)
}
