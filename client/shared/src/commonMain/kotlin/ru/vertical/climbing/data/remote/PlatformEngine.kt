package ru.vertical.climbing.data.remote

import io.ktor.client.engine.HttpClientEngine

/** Реальный сетевой движок Ktor для платформы (OkHttp / Darwin). */
expect fun createPlatformEngine(): HttpClientEngine
