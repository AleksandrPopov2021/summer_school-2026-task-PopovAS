# BUG-001 — Flaky integration-тест отказа без допуска на «трассы»

**Статус:** Исправлено  
**Компонент:** Backend — `booking_integration_test.go`  
**Связанные кейсы:** PYR-INT-BE-003, FR-009, UC-002

---

## Проблема

Тест `TestCreateBookingWithoutClearanceOnRopeIntegration` падал нестабильно в зависимости от времени суток:

```
expected 403, got 422 body={"code":"BOOKING_CUTOFF_EXCEEDED",...}
```

**Причина:** хелпер `findRopeSlotWithSpots` брал первый rope-слот из seed с `free_spots > 0`, не проверяя окно записи (`within_booking_window`). После начала слота (или за 30 мин до него) API возвращал **422** (`BOOKING_CUTOFF_EXCEEDED`) раньше, чем **403** (`INSTRUCTOR_CLEARANCE_REQUIRED`), потому что в `validateSlotForBooking` cutoff проверяется первым.

Тест проверял сценарий «нет допуска», но фактически бронировал слот, уже недоступный для записи.

---

## Решение

1. **Детерминированный слот для теста** — добавлен `insertRopeSlotWithSpots`: вставляет rope-слот с `starts_at = now + 3h` (вне cutoff), зона `rope_routes`, без зависимости от seed и времени прогона.

2. **Явная проверка кода ошибки** — после ответа 403 проверяется `INSTRUCTOR_CLEARANCE_REQUIRED`.

3. **Защита хелперов** — в `findRopeSlotWithSpots` и `findBoulderSlotWithSpots` добавлен фильтр `item.Availability.WithinBookingWindow` для остальных тестов, использующих seed.

**Изменённые файлы:**

- `backend/internal/adapters/http/booking_integration_test.go`

**Проверка:**

```bash
cd backend
go test ./internal/adapters/http -run TestCreateBookingWithoutClearanceOnRopeIntegration -count=1
```
