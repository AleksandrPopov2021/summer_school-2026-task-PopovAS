# BUG-002 — Неверное ожидание в `PushRoutingTest.deep_link_alternative`

**Статус:** Исправлено  
**Компонент:** Client shared — `PushRoutingTest.kt`  
**Связанные кейсы:** PYR-UNIT-CL-003, LOGIC-013

---

## Проблема

Unit-тест `deep_link_alternative` ожидал:

```kotlin
parseDeepLink("vertical://bookings/b1/alternative")
// → AlternativeSlot("b1", "s9")
```

Фактически `parseDeepLink` возвращал `AlternativeSlot("b1", "b1")`.

**Причина:** по спецификации LOGIC-013 deep link `vertical://bookings/{booking_id}/alternative` содержит только `booking_id`, без `slot_id`. Реализация в `PushNotification.kt` использует 4-й сегмент URL как `cancelledSlotId`, а при его отсутствии подставляет `bookingId`:

```kotlin
cancelledSlotId = segments.getOrNull(3).orEmpty().ifBlank { bookingId }
```

Значение `"s9"` в тесте нигде не задавалось — это ошибка теста, а не логики.

---

## Решение

Тест приведён в соответствие со спецификацией и реализацией:

1. URL **без** slot id → `AlternativeSlot("b1", "b1")` (fallback на `bookingId` до загрузки контекста записи).
2. URL **с** slot id → `vertical://bookings/b1/alternative/s9` → `AlternativeSlot("b1", "s9")`.

**Изменённые файлы:**

- `client/shared/src/commonTest/kotlin/ru/vertical/climbing/domain/PushRoutingTest.kt`

**Проверка:**

```bash
cd client
./gradlew :shared:testDebugUnitTest --tests "ru.vertical.climbing.domain.PushRoutingTest"
```
