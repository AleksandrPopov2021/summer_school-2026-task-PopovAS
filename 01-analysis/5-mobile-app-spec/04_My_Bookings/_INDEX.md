# 04. Мои записи

Домен мобильного приложения «Вертикаль»: просмотр активных записей, управление прокатом, отмена и предложение альтернативного слота.

**Релиз:** 1.0.0  
**Статус раздела:** Актуален

---

## Экраны

| ID | Название | Файл | Приоритет | Статус |
|----|----------|------|-----------|--------|
| SCR-006 | Список моих записей | [SCR-006_My-Bookings-Screen.md](SCR-006_My-Bookings-Screen.md) | Critical | Актуален |
| SCR-007 | Детали записи | [SCR-007_Booking-Detail-Screen.md](SCR-007_Booking-Detail-Screen.md) | Critical | Актуален |
| SCR-008 | Подтверждение отмены | [SCR-008_Cancellation-Confirmation-Screen.md](SCR-008_Cancellation-Confirmation-Screen.md) | High | Актуален |
| SCR-009 | Альтернативный слот | [SCR-009_Alternative-Slot-Offer-Screen.md](SCR-009_Alternative-Slot-Offer-Screen.md) | High | Актуален |

---

## Применяемые логики

| Логика | Экраны | Описание |
|--------|--------|----------|
| [LOGIC-007](../09_Logics/LOGIC-007_Выбор-проката-и-расчёт-стоимости.md) | SCR-007 | Отображение и пересчёт стоимости проката |
| [LOGIC-008](../09_Logics/LOGIC-008_Отмена-записи-с-учётом-политики.md) | SCR-006, SCR-007, SCR-008 | Правила отмены по времени до начала |
| [LOGIC-009](../09_Logics/LOGIC-009_Предложение-альтернативного-слота.md) | SCR-009 | Поиск и предложение замены при отмене скалодромом |
| [LOGIC-010](../09_Logics/LOGIC-010_Изменение-проката-в-записи.md) | SCR-007 | Изменение позиций проката в активной записи |

---

## Связанные FR

FR-016, FR-017, FR-018, FR-019, FR-020, FR-021, FR-022, FR-023, FR-028

---

## API

Спецификация: [openapi.yaml](../../api/openapi.yaml)

| operationId | Метод | Путь |
|-------------|-------|------|
| `listBookings` | GET | `/bookings?status=booked` |
| `getBooking` | GET | `/bookings/{bookingId}` |
| `cancelBooking` | DELETE | `/bookings/{bookingId}` |
| `updateBookingRental` | PATCH | `/bookings/{bookingId}/rental` |
| `findAlternativeSlot` | GET | `/slots/alternatives?cancelled_slot_id=&booking_id=` |
