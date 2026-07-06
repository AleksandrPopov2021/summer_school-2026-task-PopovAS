# План реализации backend — итеративный чек-лист

**Проект:** Скалодром «Вертикаль»  
**Контракт API:** [`01-analysis/api/openapi.yaml`](../01-analysis/api/openapi.yaml)  
**Модель данных:** [`01-analysis/04-design/entity-models.md`](../01-analysis/04-design/entity-models.md)

Формат: **итерация → чек-лист задач → критерий «готово»**.

**Стек:** Go, chi, oapi-codegen, PostgreSQL, sqlc, goose, JWT.

**Архитектура:** Hexagonal (Ports & Adapters).

---

## Итерация 0 — Каркас проекта

### Инфраструктура

- [ ] Создать `backend/` (hexagonal: `cmd/`, `internal/domain`, `application`, `ports`, `adapters`)
- [ ] Подключить `openapi.yaml` (symlink или копия из `01-analysis/api/`)
- [ ] Настроить **oapi-codegen** (types + server interface)
- [ ] Настроить **chi** + middleware (logging, recovery, request ID, CORS)
- [ ] Единый mapper ошибок → `ErrorResponse` (NFR-003)
- [ ] `docker-compose.yml`: PostgreSQL
- [ ] **goose** + команда `make migrate`
- [ ] `Makefile`: `run`, `test`, `migrate`, `seed`, `generate`
- [ ] `.env.example` + загрузка конфига

### База — справочники (DB-0)

- [ ] Миграция: `gym_venues`
- [ ] Миграция: `training_zones` (`format_type`, `difficulty`, `max_group_size`)
- [ ] Миграция: `instructors`
- [ ] Миграция: `rental_equipment_types` (shoes, harness, helmet, chalk)
- [ ] Миграция: `cancellation_reasons`
- [ ] Миграция: `system_config` (singleton)

### Seed (минимум)

- [ ] Скалодром «Вертикаль»
- [ ] 2 зоны: болдеринг (beginner, max 8) + трассы (experienced, max 16)
- [ ] 3–5 инструкторов
- [ ] 4 типа проката
- [ ] 3–4 причины отмены скалодромом
- [ ] `system_config`: cutoff 30 мин, cancel 60 мин, reminder N ч, loyalty N, sanctions N

### API — read-only reference

- [ ] `GET /config` → `SystemConfig`
- [ ] `GET /rental-equipment-types` → справочник

**✅ Готово, когда:** `docker compose up` + `make migrate seed` + оба endpoint отвечают по схеме OpenAPI.

---

## Итерация 1 — Клиент и авторизация

### База (DB-1)

- [ ] Миграция: `clients`
- [ ] Миграция: `notification_preferences` (1:1)
- [ ] Миграция: `device_push_tokens` (структура, используется позже)

### Auth

- [ ] JWT: выпуск при регистрации, middleware `Bearer`
- [ ] Извлечение `client_id` из токена во всех protected handlers

### Use cases

- [ ] `RegisterClient` — валидация E.164, уникальность phone
- [ ] Создание default `notification_preferences` (reminders=true, gym_cancellation=true)
- [ ] `GetCurrentClient`
- [ ] `UpdateRiskConsent` — только `risk_consent_accepted: true`

### API

- [ ] `POST /clients` → 201 + `access_token` + `Client`
- [ ] `POST /clients` → 409 `CLIENT_ALREADY_EXISTS`
- [ ] `GET /clients/me` → 200 / 401
- [ ] `PATCH /clients/me` → обновление risk consent

### Notification preferences

- [ ] `GET /clients/me/notification-preferences`
- [ ] `PATCH /clients/me/notification-preferences` — только отключаемые поля (BR-028, BR-029)
- [ ] Отклонять попытку изменить `reminders_enabled` / `gym_cancellation_enabled`

### Тесты

- [ ] Unit: валидация phone, JWT roundtrip
- [ ] Integration: register → me → patch consent

**✅ Готово, когда:** mobile может пройти SCR-001/002, LOGIC-001, LOGIC-002, LOGIC-011 (read).

---

## Итерация 2 — Расписание слотов

### База (DB-2)

- [ ] Миграция: `training_slots` (capacity, free_spots, prices, slot_status, FK)
- [ ] Миграция: `slot_rental_availability`
- [ ] Индексы по `starts_at`, `(slot_status, starts_at)`

### Seed

- [ ] Слоты на 7–14 дней вперёд (разные зоны, инструкторы, free_spots)
- [ ] Прокатный фонд на каждый слот
- [ ] 1–2 слота со статусом `cancelled_by_gym` (для FR-006)

### Domain

- [ ] `BuildBookingAvailability(slot, client?, config, clearances)`:
  - [ ] `has_free_spots`
  - [ ] `within_booking_window` (BR-006)
  - [ ] `clearance_required` для `rope_routes` (BR-007)
  - [ ] `clearance_granted`
  - [ ] `can_book` — итоговый флаг

### API

- [ ] `GET /slots` — default `from=today`, `to=from+7d` (BR-027)
- [ ] `GET /slots?date=` — фильтр одного дня (FR-002)
- [ ] `GET /slots` — включает `cancelled_by_gym` (BR-019)
- [ ] `GET /slots/{slotId}` → `TrainingSlotDetail` + rental_availability
- [ ] `availability` в каждом слоте (для авторизованного — с учётом допуска)

### База (DB-1 доп.)

- [ ] Миграция: `instructor_clearances`
- [ ] Seed: допуск для тестового клиента

### API — допуски

- [ ] `GET /clients/me/clearances`

### Тесты

- [ ] Unit: `BookingAvailability` — boulder / rope / no spots / cutoff
- [ ] Integration: list slots за 7 дней, cancelled visible

**✅ Готово, когда:** mobile может реализовать SCR-003, SCR-004, SCR-013, LOGIC-003, LOGIC-004.

---

## Итерация 3 — Создание записи (критическая)

### База (DB-3)

- [ ] Миграция: `bookings`
- [ ] Миграция: `booking_rental_lines`
- [ ] Миграция: `payments`

### Domain / pricing

- [ ] Расчёт: training + rental − loyalty discount → `PaymentInfo`
- [ ] Snapshot `unit_price` в rental lines

### Use case `CreateBooking` (транзакция)

- [ ] `SELECT slot FOR UPDATE`
- [ ] Проверка `risk_consent_accepted` → 403 `RISK_CONSENT_REQUIRED` (BR-031)
- [ ] Проверка `slot_status == active`
- [ ] Проверка `free_spots > 0` → 422 `NO_FREE_SPOTS` (BR-008)
- [ ] Проверка cutoff → 422 `BOOKING_CUTOFF_EXCEEDED` (BR-006)
- [ ] Проверка допуска для rope → 403 `INSTRUCTOR_CLEARANCE_REQUIRED` (BR-007)
- [ ] Проверка `rebooking_forbidden` → 422 (BR-018)
- [ ] Резервирование проката из `slot_rental_availability`
- [ ] `free_spots -= 1`
- [ ] INSERT booking + rental_lines + payment (`unpaid`, BR-024)
- [ ] Гонка → 409 + `BookingConflictResponse` с актуальным слотом (BR-003, BR-004, FR-014)

### API

- [ ] `POST /bookings` → 201 `BookingDetail`
- [ ] `POST /bookings` → все коды: 400, 401, 403, 409, 422
- [ ] `GET /slots/{slotId}/rental-availability` (JWT)

### Тесты

- [ ] Integration: parallel `POST /bookings` на последнее место → один 201, один 409
- [ ] Integration: book without consent / without clearance / after cutoff

**✅ Готово, когда:** mobile может пройти SCR-005, SCR-015, LOGIC-005–007.

---

## Итерация 4 — Мои записи и отмена

### Domain

- [ ] `BuildCancellationPolicy(booking, slot, config)`:
  - [ ] `none` — > 2 ч (BR-010)
  - [ ] `late_cancellation` — 1–2 ч (BR-011)
  - [ ] `forbidden` — < 1 ч (BR-012)

### Use cases

- [ ] `ListBookings` — default `status=booked`
- [ ] `GetBooking` — slot + payment + rental_lines + cancellation_policy
- [ ] `CancelBooking`:
  - [ ] только `booking_status == booked`
  - [ ] forbidden → 403 `CANCELLATION_FORBIDDEN`
  - [ ] already cancelled → 409
  - [ ] status → `cancelled_by_client`, `free_spots++`, release rental
  - [ ] increment `late_cancellation_count` при отмене 1–2 ч (BR-014)

### API

- [ ] `GET /bookings?status=`
- [ ] `GET /bookings/{bookingId}`
- [ ] `DELETE /bookings/{bookingId}` → 200 / 403 / 404 / 409

### Тесты

- [ ] Cancel at 90 min → 200 + late count++
- [ ] Cancel at 59 min → 403
- [ ] Cancel at 3 h → 200, spot released

**✅ Готово, когда:** mobile может пройти SCR-006–008, LOGIC-008.

---

## Итерация 5 — Изменение проката

### Use case `UpdateBookingRental`

- [ ] Только `booking_status == booked`
- [ ] Diff rental: освободить старые + зарезервировать новые
- [ ] Учесть уже занятые этой записью позиции
- [ ] Недостаточно фонда → 422 `RENTAL_UNAVAILABLE`
- [ ] Пересчёт `PaymentInfo`

### API

- [ ] `PATCH /bookings/{bookingId}/rental` → 200 / 403 / 404 / 422

### Тесты

- [ ] Смена проката при достаточном фонде
- [ ] Отказ при исчерпанном фонде

**✅ Готово, когда:** mobile может пройти LOGIC-010, часть SCR-007.

---

## Итерация 6 — Отмена скалодромом и альтернативный слот

### Internal (вне клиентского OpenAPI)

- [ ] `POST /internal/slots/{id}/cancel-by-gym` (API key, dev/staging)
- [ ] Use case `CancelSlotByGym`:
  - [ ] `slot_status = cancelled_by_gym` (BR-016)
  - [ ] все `booked` → `cancelled_by_gym` + `rebooking_forbidden=true` (BR-018)
  - [ ] `payment_status = refund` если `paid` (BR-021)
  - [ ] push всем затронутым (BR-017) — пока log/noop

### Use case `FindAlternativeSlot`

- [ ] `GET /slots/alternatives?cancelled_slot_id=&booking_id=`
- [ ] Поиск ближайшего active: та же zone, instructor, format
- [ ] `free_spots > 0`, rental покрывает lines из booking
- [ ] `found: false` — валидный ответ (BR-020)

### API

- [ ] `GET /slots/alternatives` → 200 / 401 / 404

### Тесты

- [ ] E2E: book → gym cancel → GET booking (reason + refund)
- [ ] `rebooking_forbidden`: повторный POST на тот же slot → отказ
- [ ] alternatives found / not found

**✅ Готово, когда:** mobile может пройти SCR-009, LOGIC-009, часть SCR-014.

---

## Итерация 7 — Push-токены и фоновые задачи

### Push token

- [ ] `PUT /devices/push-token` — upsert token + platform
- [ ] Интерфейс `PushSender` (Noop / Logging / FCM+APNs)

### Worker (`cmd/worker`)

- [ ] Job: напоминание за 24 ч (BR-027, обязательное)
- [ ] Job: напоминание за `reminder_hours_before` ч (BR-027)
- [ ] Job: `CompleteBookings` — `booked` → `completed`, increment visits
- [ ] Job: пересчёт `is_loyal_client` + `loyalty_discount` (BR-032)
- [ ] Job (optional): `no_show` + increment counter (BR-015)

### Push-события

- [ ] Booking created — если `booking_confirmation_enabled` (BR-029)
- [ ] Gym cancellation — всегда (BR-028)
- [ ] Reminders — всегда (BR-028)
- [ ] Payload для deep link: `{ type, booking_id, slot_id }`

### API

- [ ] `PUT /devices/push-token` → 200 / 401

### Тесты

- [ ] Worker: complete booking → loyalty badge
- [ ] Push не шлётся при отключённом booking_confirmation

**✅ Готово, когда:** mobile может пройти LOGIC-012, LOGIC-013, SCR-011, SCR-014.

---

## Итерация 8 — Санкции и hardening

### Санкции (BR-014, BR-015)

- [ ] Блокировка `POST /bookings` при `late_cancellation_count + no_show_count >= violations_for_sanctions`
- [ ] Код ошибки: `403 BOOKING_FORBIDDEN_SANCTIONS`

### Качество

- [ ] Contract tests по OpenAPI (все MVP paths + status codes)
- [ ] Load test: 50 concurrent `POST /bookings`
- [ ] `backend/README.md`: setup за 5 минут
- [ ] CI: lint, test, migrate on empty DB

### Деплой

- [ ] Staging: Postgres + API + worker
- [ ] Передать mobile-команде base URL + test credentials

**✅ MVP backend freeze.**

---

## Post-MVP

- [ ] Миграция: `instructor_ratings`
- [ ] `POST /ratings` — окно 1–2 суток (BR-033)
- [ ] Блокировка при `cancelled_by_gym` → `RATING_NOT_ALLOWED` (BR-034)
- [ ] Дубликат → 409
- [ ] Пересчёт `instructor.average_rating` (BR-035)
- [ ] Push «оцените инструктора» (BR-029)

---

## Сводная таблица итераций

| # | Фокус | Unblock для mobile |
|---|--------|-------------------|
| 0 | Каркас + справочники | — |
| 1 | Auth + профиль | SCR-001, SCR-002 |
| 2 | Расписание | SCR-003, SCR-004, SCR-013 |
| 3 | Бронирование | SCR-005, SCR-015 |
| 4 | Отмена | SCR-006–008 |
| 5 | Прокат | SCR-007 |
| 6 | Gym cancel + альтернатива | SCR-009 |
| 7 | Push + worker | SCR-011, SCR-014 |
| 8 | Hardening + санкции | release |

---

## Финальный чек-лист «backend MVP готов»

- [ ] 18 MVP endpoints из OpenAPI работают
- [ ] BR-004: concurrency-тест пройден
- [ ] BR-006, BR-007, BR-012 enforced на сервере
- [ ] `BookingAvailability` и `CancellationPolicy` в ответах
- [ ] Gym cancel E2E через internal API
- [ ] Worker: reminders + complete + loyalty
- [ ] Seed воспроизводим: `migrate` + `seed` с нуля
- [ ] README + staging URL

---

## Связанные документы

- [План реализации клиента](./client-implementation-plan.md)
- [OpenAPI](../01-analysis/api/openapi.yaml)
- [Entity models](../01-analysis/04-design/entity-models.md)
- [Sequence diagrams](../01-analysis/04-design/sequence-diagrams.md)
