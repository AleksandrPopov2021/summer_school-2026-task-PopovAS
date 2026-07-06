package ru.vertical.climbing

import android.content.Context
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertTrue
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.rules.ExternalResource
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import ru.vertical.climbing.app.di.initKoin
import ru.vertical.climbing.data.remote.ApiConfig
import ru.vertical.climbing.domain.model.ClientRegistration
import ru.vertical.climbing.domain.usecase.RegisterClientUseCase
import ru.vertical.climbing.domain.util.AppResult

/**
 * Android UI smoke-тесты (mock API, Итерация 9).
 *
 * Полный сценарий «запись → отмена» через UI пока нестабилен из‑за ограничений accessibility
 * Compose Multiplatform (см. [BUG-003-android-ui-espresso-inputmanager.md]).
 */
@RunWith(AndroidJUnit4::class)
class MvpFlowTest {

    private val device: UiDevice
        get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @get:Rule(order = 0)
    val resetStateRule = object : ExternalResource() {
        override fun before() {
            clearAppPreferences()
            ensureKoin()
            registerTestClient()
        }
    }

    @get:Rule(order = 1)
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun authenticated_user_sees_schedule_slots() {
        assertTrue("Schedule tab expected", device.wait(Until.hasObject(By.text("Расписание")), 20_000))
        selectScheduleDay(SCHEDULE_DAY_OFFSET)
        assertTrue(
            "Slot card expected on selected day",
            device.wait(Until.hasObject(By.textContains("Болдеринг")), 15_000),
        )
    }

    @Ignore("Compose CMP не экспортирует кнопку «Записаться» в a11y-tree — см. BUG-003")
    @Test
    fun register_book_and_cancel() {
        assertTrue(device.wait(Until.hasObject(By.text("Расписание")), 20_000))
        selectScheduleDay(SCHEDULE_DAY_OFFSET)
        assertTrue(device.wait(Until.hasObject(By.desc("Записаться")), 15_000))
        device.findObject(By.desc("Записаться"))?.click()
        if (device.wait(Until.hasObject(By.text("Согласие на риск")), 5_000)) {
            device.findObject(By.text("Я подтверждаю, что ознакомлен(а) и принимаю риски"))?.click()
            device.findObject(By.text("Подтвердить"))?.click()
        }
        device.wait(Until.hasObject(By.text("Подтвердить запись")), 10_000)
        device.findObject(By.text("Подтвердить запись"))?.click()
        device.findObject(By.text("Мои записи"))?.click()
        device.findObject(By.textContains("Болдеринг"))?.click()
        device.findObject(By.text("Отменить запись"))?.click()
        device.findObject(By.text("Подтвердить отмену"))?.click()
    }

    private fun selectScheduleDay(offset: Int) {
        val chips = device.findObjects(By.clickable(true)).filter { node ->
            node.text == "Сегодня" || (node.text?.contains(",") == true && (node.text?.length ?: 0) < 24)
        }
        chips.getOrNull(offset)?.click()
        device.waitForIdle()
    }

    private fun registerTestClient() {
        runBlocking {
            val result = GlobalContext.get().get<RegisterClientUseCase>()(
                ClientRegistration(
                    phone = "+79001234567",
                    fullName = "Тестов Тест Тестович",
                    birthDate = LocalDate(1990, 1, 15),
                ),
            )
            check(result is AppResult.Success) { "Test client registration failed: $result" }
        }
    }

    private fun ensureKoin() {
        GlobalContext.stopKoin()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        initKoin(apiConfig = ApiConfig(useMock = true)) {
            androidContext(context.applicationContext)
        }
    }

    private fun clearAppPreferences() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.getSharedPreferences("${context.packageName}_preferences", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    private companion object {
        /** Mock-фикстуры slot-1 на day+2 относительно «сегодня». */
        const val SCHEDULE_DAY_OFFSET = 2
    }
}
