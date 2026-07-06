package ru.vertical.climbing.di

import kotlinx.serialization.json.Json
import org.koin.core.module.Module
import org.koin.dsl.module
import ru.vertical.climbing.data.local.ApiEnvStore
import ru.vertical.climbing.data.local.LocalCache
import ru.vertical.climbing.data.local.PlatformSettings
import ru.vertical.climbing.data.local.TokenStore
import ru.vertical.climbing.data.remote.ApiConfig
import ru.vertical.climbing.data.remote.HttpClientFactory
import ru.vertical.climbing.data.remote.appJson
import ru.vertical.climbing.data.repository.AuthRepositoryImpl
import ru.vertical.climbing.data.repository.BookingRepositoryImpl
import ru.vertical.climbing.data.repository.ClearanceRepositoryImpl
import ru.vertical.climbing.data.repository.ConfigRepositoryImpl
import ru.vertical.climbing.data.repository.DeviceRepositoryImpl
import ru.vertical.climbing.data.repository.NotificationPreferencesRepositoryImpl
import ru.vertical.climbing.data.repository.RatingRepositoryImpl
import ru.vertical.climbing.data.repository.ReferenceRepositoryImpl
import ru.vertical.climbing.data.repository.SlotRepositoryImpl
import ru.vertical.climbing.domain.repository.AuthRepository
import ru.vertical.climbing.domain.repository.BookingRepository
import ru.vertical.climbing.domain.repository.ClearanceRepository
import ru.vertical.climbing.domain.repository.ConfigRepository
import ru.vertical.climbing.domain.repository.DeviceRepository
import ru.vertical.climbing.domain.repository.NotificationPreferencesRepository
import ru.vertical.climbing.domain.repository.RatingRepository
import ru.vertical.climbing.domain.repository.ReferenceRepository
import ru.vertical.climbing.domain.repository.SlotRepository
import ru.vertical.climbing.domain.usecase.AcceptRiskConsentUseCase
import ru.vertical.climbing.domain.usecase.CalculateBookingPriceUseCase
import ru.vertical.climbing.domain.usecase.CancelBookingUseCase
import ru.vertical.climbing.domain.usecase.CheckSessionUseCase
import ru.vertical.climbing.domain.usecase.CreateBookingUseCase
import ru.vertical.climbing.domain.usecase.FindAlternativeSlotUseCase
import ru.vertical.climbing.domain.usecase.GetBookingDetailUseCase
import ru.vertical.climbing.domain.usecase.GetRebookingForbiddenSlotIdsUseCase
import ru.vertical.climbing.domain.usecase.PrefillBookingDraftFromBookingUseCase
import ru.vertical.climbing.domain.usecase.PushRegistrationCoordinator
import ru.vertical.climbing.domain.usecase.RegisterPushTokenUseCase
import ru.vertical.climbing.domain.usecase.GetNotificationPreferencesUseCase
import ru.vertical.climbing.domain.usecase.GetProfileUseCase
import ru.vertical.climbing.domain.usecase.GetSlotDetailUseCase
import ru.vertical.climbing.domain.usecase.GetSlotRentalAvailabilityUseCase
import ru.vertical.climbing.domain.usecase.ListMyBookingsUseCase
import ru.vertical.climbing.domain.usecase.LoadBookingDraftUseCase
import ru.vertical.climbing.domain.usecase.LoadProfileUseCase
import ru.vertical.climbing.domain.usecase.LoadScheduleUseCase
import ru.vertical.climbing.domain.usecase.LoadRatingContextUseCase
import ru.vertical.climbing.domain.usecase.ListRateableBookingsUseCase
import ru.vertical.climbing.domain.usecase.ListRentalEquipmentTypesUseCase
import ru.vertical.climbing.domain.usecase.RegisterClientUseCase
import ru.vertical.climbing.domain.usecase.SaveBookingDraftUseCase
import ru.vertical.climbing.domain.usecase.SignOutUseCase
import ru.vertical.climbing.domain.usecase.SubmitRatingUseCase
import ru.vertical.climbing.domain.usecase.UpdateBookingRentalUseCase
import ru.vertical.climbing.domain.usecase.UpdateNotificationPreferencesUseCase

/**
 * Точка сборки Koin-графа `shared`. [apiConfigOverride] позволяет платформе
 * переключать окружение / mock-режим (debug-меню, Итерация 9).
 */
fun sharedModules(apiConfigOverride: ApiConfig? = null): List<Module> = listOf(
    coreModule(apiConfigOverride),
    networkModule,
    repositoryModule,
    useCaseModule,
    platformModule,
)

private fun coreModule(apiConfigOverride: ApiConfig?): Module = module {
    single { ApiEnvStore(PlatformSettings.cache()) }
    single { apiConfigOverride ?: get<ApiEnvStore>().load() }
    single<Json> { appJson }
    single { TokenStore(PlatformSettings.secure()) }
    single { LocalCache(PlatformSettings.cache(), get()) }
}

private val networkModule: Module = module {
    single { HttpClientFactory.create(config = get(), tokenStore = get(), json = get()) }
}

private val repositoryModule: Module = module {
    single<AuthRepository> { AuthRepositoryImpl(get(), get(), get()) }
    single<ConfigRepository> { ConfigRepositoryImpl(get(), get(), get()) }
    single<SlotRepository> { SlotRepositoryImpl(get(), get(), get()) }
    single<BookingRepository> { BookingRepositoryImpl(get(), get(), get()) }
    single<ReferenceRepository> { ReferenceRepositoryImpl(get(), get()) }
    single<NotificationPreferencesRepository> { NotificationPreferencesRepositoryImpl(get(), get()) }
    single<ClearanceRepository> { ClearanceRepositoryImpl(get(), get()) }
    single<DeviceRepository> { DeviceRepositoryImpl(get(), get()) }
    single<RatingRepository> { RatingRepositoryImpl(get(), get(), get()) }
}

private val useCaseModule: Module = module {
    factory { CheckSessionUseCase(get(), get()) }
    factory { RegisterClientUseCase(get()) }
    factory { AcceptRiskConsentUseCase(get()) }
    factory { GetProfileUseCase(get()) }
    factory { LoadProfileUseCase(get(), get()) }
    factory { SignOutUseCase(get()) }
    factory { GetNotificationPreferencesUseCase(get()) }
    factory { UpdateNotificationPreferencesUseCase(get()) }
    factory { LoadScheduleUseCase(get()) }
    factory { GetSlotDetailUseCase(get()) }
    factory { GetSlotRentalAvailabilityUseCase(get()) }
    factory { ListMyBookingsUseCase(get()) }
    factory { GetBookingDetailUseCase(get()) }
    factory { CreateBookingUseCase(get()) }
    factory { ListRentalEquipmentTypesUseCase(get()) }
    factory { SaveBookingDraftUseCase(get()) }
    factory { LoadBookingDraftUseCase(get()) }
    factory { CancelBookingUseCase(get()) }
    factory { UpdateBookingRentalUseCase(get()) }
    factory { FindAlternativeSlotUseCase(get()) }
    factory { PrefillBookingDraftFromBookingUseCase(get()) }
    factory { GetRebookingForbiddenSlotIdsUseCase(get()) }
    factory { CalculateBookingPriceUseCase() }
    factory { RegisterPushTokenUseCase(get(), get(), get()) }
    factory { LoadRatingContextUseCase(get(), get()) }
    factory { SubmitRatingUseCase(get()) }
    factory { ListRateableBookingsUseCase(get(), get()) }
    single { PushRegistrationCoordinator(get(), get()) }
}
