package ru.vertical.climbing.app.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration
import ru.vertical.climbing.data.remote.ApiConfig
import ru.vertical.climbing.di.sharedModules

/** Инициализация Koin-графа. Вызывается из платформенных точек входа. */
fun initKoin(
    apiConfig: ApiConfig? = null,
    appDeclaration: KoinAppDeclaration = {},
) = startKoin {
    appDeclaration()
    modules(sharedModules(apiConfig))
}
