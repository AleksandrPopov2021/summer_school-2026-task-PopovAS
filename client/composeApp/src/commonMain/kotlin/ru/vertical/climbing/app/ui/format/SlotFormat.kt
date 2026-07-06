package ru.vertical.climbing.app.ui.format

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val zone: TimeZone get() = TimeZone.currentSystemDefault()

private val monthGenitive = mapOf(
    Month.JANUARY to "января",
    Month.FEBRUARY to "февраля",
    Month.MARCH to "марта",
    Month.APRIL to "апреля",
    Month.MAY to "мая",
    Month.JUNE to "июня",
    Month.JULY to "июля",
    Month.AUGUST to "августа",
    Month.SEPTEMBER to "сентября",
    Month.OCTOBER to "октября",
    Month.NOVEMBER to "ноября",
    Month.DECEMBER to "декабря",
)

private val monthShort = mapOf(
    Month.JANUARY to "янв",
    Month.FEBRUARY to "фев",
    Month.MARCH to "мар",
    Month.APRIL to "апр",
    Month.MAY to "мая",
    Month.JUNE to "июн",
    Month.JULY to "июл",
    Month.AUGUST to "авг",
    Month.SEPTEMBER to "сен",
    Month.OCTOBER to "окт",
    Month.NOVEMBER to "ноя",
    Month.DECEMBER to "дек",
)

private val weekdayShort = mapOf(
    DayOfWeek.MONDAY to "Пн",
    DayOfWeek.TUESDAY to "Вт",
    DayOfWeek.WEDNESDAY to "Ср",
    DayOfWeek.THURSDAY to "Чт",
    DayOfWeek.FRIDAY to "Пт",
    DayOfWeek.SATURDAY to "Сб",
    DayOfWeek.SUNDAY to "Вс",
)

private val weekdayFull = mapOf(
    DayOfWeek.MONDAY to "Понедельник",
    DayOfWeek.TUESDAY to "Вторник",
    DayOfWeek.WEDNESDAY to "Среда",
    DayOfWeek.THURSDAY to "Четверг",
    DayOfWeek.FRIDAY to "Пятница",
    DayOfWeek.SATURDAY to "Суббота",
    DayOfWeek.SUNDAY to "Воскресенье",
)

/** Подпись chip даты в переключателе: «Пн, 15 июл». */
fun LocalDate.chipLabel(): String = "${weekdayShort[dayOfWeek]}, $dayOfMonth ${monthShort[month]}"

/** Заголовок деталей: «Суббота, 10 июля». */
fun Instant.heroDateLabel(): String {
    val date = toLocalDateTime(zone).date
    return "${weekdayFull[date.dayOfWeek]}, ${date.dayOfMonth} ${monthGenitive[date.month]}"
}

/** Время начала «HH:mm». */
fun Instant.timeLabel(): String {
    val time = toLocalDateTime(zone).time
    return "${time.hour.pad()}:${time.minute.pad()}"
}

/** Время окончания = начало + длительность. */
fun endTimeLabel(start: Instant, durationMinutes: Int): String =
    start.plusMinutes(durationMinutes).timeLabel()

/** Человеко-читаемая длительность: «1 ч 30 мин», «90 мин» → «1 ч 30 мин». */
fun durationLabel(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours == 0 -> "$rest мин"
        rest == 0 -> "$hours ч"
        else -> "$hours ч $rest мин"
    }
}

private fun Instant.plusMinutes(minutes: Int): Instant =
    Instant.fromEpochMilliseconds(toEpochMilliseconds() + minutes * 60_000L)

private fun Int.pad(): String = toString().padStart(2, '0')
