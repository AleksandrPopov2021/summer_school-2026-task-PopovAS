package ru.vertical.climbing

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import ru.vertical.climbing.app.di.initKoin
import ru.vertical.climbing.domain.usecase.PushRegistrationCoordinator
import ru.vertical.climbing.push.AndroidNotificationChannels
import ru.vertical.climbing.push.PushTokenRefreshHost

class VerticalApplication : Application(), PushTokenRefreshHost {

    private val pushRegistration: PushRegistrationCoordinator by inject()
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidLogger()
            androidContext(this@VerticalApplication)
        }
        AndroidNotificationChannels.ensureCreated(this)
    }

    override fun onPushTokenRefreshed(token: String) {
        appScope.launch {
            pushRegistration.onTokenRefreshed(token)
        }
    }
}
