package ru.vertical.climbing.di

import org.koin.core.module.Module
import org.koin.dsl.module
import ru.vertical.climbing.push.IosPushTokenProvider
import ru.vertical.climbing.push.PushTokenProvider

actual val platformModule: Module = module {
    single<PushTokenProvider> { IosPushTokenProvider() }
}
