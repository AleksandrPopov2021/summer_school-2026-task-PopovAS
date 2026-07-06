package ru.vertical.climbing.di

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module
import ru.vertical.climbing.push.AndroidPushTokenProvider
import ru.vertical.climbing.push.PushTokenProvider

actual val platformModule: Module = module {
    single<PushTokenProvider> { AndroidPushTokenProvider(androidContext()) }
}
