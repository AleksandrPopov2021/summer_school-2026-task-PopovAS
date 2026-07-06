# План реализации клиента (CMP) — итеративный чек-лист

**Проект:** Скалодром «Вертикаль»  
**Спецификация UI/логик:** [`01-analysis/5-mobile-app-spec/`](../01-analysis/5-mobile-app-spec/)  
**Контракт API:** [`01-analysis/api/openapi.yaml`](../01-analysis/api/openapi.yaml)

Формат: **итерация → чек-лист → критерий «готово»**.

**Стек:** Compose Multiplatform, Decompose, Koin, Ktor, kotlinx.serialization, Clean/MVI.

---

## Итерация 0 — Каркас приложения

### Проект

- [x] Создать проект (KMP + CMP) в [`client/`](../client/)
- [x] Модули: `composeApp`, `shared` (domain / data / presentation), `iosApp` (Android — в `composeApp/androidMain`)
- [x] Подключить Compose Multiplatform, Material 3, тема «Вертикаль»
- [x] Русская локализация строк (NFR-005) — единый `strings` resource

### Архитектура shared

- [x] `domain/model` — Client, Slot, Booking, enums
- [x] `domain/repository` — интерфейсы
- [x] `domain/usecase` — скелет use cases
- [x] `data/remote` — Ktor `HttpClient`, auth interceptor (Bearer)
- [x] `data/local` — secure token store (expect/actual)
- [x] `data/mapper` — DTO ↔ domain
- [x] `presentation` — базовый `UiState` / `UiEvent` / `BaseViewModel`

### DI и навигация

- [x] Koin modules: network, repositories, use cases (view models — по мере появления экранов)
- [x] Decompose: `RootComponent`, `AppRouter`
- [x] Auth flow vs Main flow (tabs)

### API-клиент

- [x] Base URL из конфига (dev/staging/prod)
- [x] Обработка `ErrorResponse` → domain errors (NFR-003)
- [x] DTO по OpenAPI (ручные для MVP endpoints)
- [x] Mock-режим для разработки до backend

### Локальное хранилище (из ТЗ)

- [x] `access_token` — secure store (expect/actual; Keychain/EncryptedSharedPreferences — Итерация 1)
- [x] `cached_config` — Settings
- [x] `cached_slots` — кэш расписания
- [x] `booking_draft` — черновик записи

### Shell UI

- [x] Bottom navigation: **Расписание** | **Мои записи** | **Профиль**
- [x] Placeholder-экраны для tabs
- [x] Loading / Error / Empty composables (переиспользуемые)

**✅ Готово, когда:** приложение собирается на Android + iOS, показывает tabs, mock API отвечает.

**Backend:** не требуется (mock). Параллельно с backend [Итерация 0](./backend-implementation-plan.md#итерация-0--каркас-проекта).

---

## Итерация 1 — Авторизация

### LOGIC-001 — Проверка сессии

- [x] SCR-001 Splash: логотип + индикатор загрузки
- [x] Cold start: чтение `access_token`
- [x] Нет токена → `GET /config` → кэш `cached_config` → SCR-002
- [x] Есть токен → `GET /clients/me`
- [x] 200 → кэш config → SCR-003 (триггер LOGIC-012 — Итерация 8)
- [x] 401 → очистить токен → SCR-002
- [x] Offline: есть `cached_config` → SCR-003 с баннером «нет сети»

### LOGIC-002 — Регистрация

- [x] SCR-002: поля телефон (E.164), ФИО, дата рождения (FR-026, BR-030)
- [x] Client-side валидация полей
- [x] `POST /clients` → сохранить token → SCR-003
- [x] 409 `CLIENT_ALREADY_EXISTS` → сообщение пользователю
- [x] Loading / error states

### Repositories / Use cases

- [x] `AuthRepository`: register, getCurrentClient, saveToken, clearToken
- [x] `ConfigRepository`: getConfig, cache
- [x] `RegisterClientUseCase`, `CheckSessionUseCase`

### Тесты

- [x] Unit: CheckSession — token valid / invalid / offline
- [x] Unit: RegisterClient — validation, 409 mapping

**✅ Готово, когда:** happy path Splash → Register → Schedule (placeholder); повторный запуск → сразу Schedule.

**Backend:** [Итерация 1](./backend-implementation-plan.md#итерация-1--клиент-и-авторизация).

---

## Итерация 2 — Расписание

### LOGIC-003 — Загрузка слотов

- [x] SCR-003 Schedule Screen
- [x] `GET /slots` — default 7 дней (BR-027, FR-001)
- [x] Переключатель дат / календарь (FR-002)
- [x] Pull-to-refresh
- [x] Кэш `cached_slots` для offline-просмотра
- [x] Loading skeleton, error retry

### LOGIC-004 — Доступность слота

- [x] Карточка слота: время, зона, инструктор, «осталось X из Y» (FR-003, BR-009)
- [x] Пометка `cancelled_by_gym` (FR-006, BR-019)
- [x] Кнопка «Записаться» по `availability.can_book` из API (FR-007–009)
- [x] Fallback UI-правила если API без auth: hide при `free_spots == 0`
- [x] SCR-013 Empty State: «Пока нет доступных тренировок» (FR-005)
- [x] Inline empty state на SCR-003 или переход на SCR-013

### SCR-004 — Детали слота

- [x] `GET /slots/{id}` → полная информация + rental_availability
- [x] Адрес скалодрома, длительность, цена, прокатный тариф
- [x] `average_rating` инструктора (если есть в API, FR-004 — optional)
- [x] Сообщение при отсутствии допуска (rope_routes)
- [x] Кнопка «Записаться» → SCR-005 (если can_book; переход в Итерации 3)
- [x] Назад → SCR-003

### Repositories / Use cases

- [x] `SlotRepository`: listSlots, getSlot, cache
- [x] `LoadScheduleUseCase`, `GetSlotDetailUseCase`
- [x] `ScheduleViewModel` / `SlotDetailViewModel` (Decompose-компоненты)

### Тесты

- [x] Unit: отображение empty state при пустом списке
- [x] Unit: кнопка записи hidden при `can_book=false`

**✅ Готово, когда:** расписание на 7 дней, навигация по датам, детали слота, empty state.

**Backend:** [Итерация 2](./backend-implementation-plan.md#итерация-2--расписание-слотов).

---

## Итерация 3 — Запись на тренировку

### LOGIC-006 — Согласие на риск

- [x] SCR-015 Consent Screen (FR-012, BR-031)
- [x] Проверка `client.risk_consent_accepted` перед записью
- [x] `PATCH /clients/me { risk_consent_accepted: true }`
- [x] Redirect: SCR-004 → SCR-015 → SCR-005 (первая запись)
- [x] 403 `RISK_CONSENT_REQUIRED` с booking flow → SCR-015

### LOGIC-007 — Прокат и стоимость

- [x] SCR-005 Booking Screen
- [x] `GET /rental-equipment-types` — справочник (FR-010)
- [x] `GET /slots/{id}/rental-availability` — доступность позиций
- [x] Toggle «Своё снаряжение» + выбор позиций проката (комбо разрешено, BR-002)
- [x] Disable позиций при `available_quantity == 0`
- [x] Разбивка: тренировка + прокат + скидка + **итого** (FR-011)
- [x] Локальный `booking_draft` до подтверждения

### LOGIC-005 — Создание записи

- [x] Кнопка «Подтвердить запись» → `POST /bookings`
- [x] 201 → success screen / redirect SCR-006, очистить draft
- [x] 409 → показать актуальный слот из `BookingConflictResponse` (FR-014)
- [x] 403 → INSTRUCTOR_CLEARANCE / RISK_CONSENT — понятные сообщения (NFR-003)
- [x] 422 → NO_FREE_SPOTS / BOOKING_CUTOFF — сообщения + обновить UI
- [x] Loading на submit, блокировка double-tap

### Repositories / Use cases

- [x] `BookingRepository`: createBooking
- [x] `RentalRepository`: equipment types, availability
- [x] `AcceptRiskConsentUseCase`, `CalculateBookingPriceUseCase`, `CreateBookingUseCase`
- [x] `BookingViewModel`

### Тесты

- [x] Unit: price calculation с loyalty discount
- [x] Unit: draft restore после kill app
- [x] Unit: mapping 409 conflict

**✅ Готово, когда:** полный flow SCR-004 → (SCR-015) → SCR-005 → запись создана → SCR-006.

**Backend:** [Итерация 3](./backend-implementation-plan.md#итерация-3--создание-записи-критическая).

---

## Итерация 4 — Мои записи и отмена

### SCR-006 — Список записей

- [x] `GET /bookings?status=booked` (FR-016)
- [x] Карточки: слот, время, статус оплаты (FR-023)
- [x] Pull-to-refresh, empty state «Нет активных записей»
- [x] Tap → SCR-007

### SCR-007 — Детали записи

- [x] `GET /bookings/{id}` — slot, rental_lines, payment, cancellation_policy
- [x] Отображение суммы и статуса оплаты: unpaid / paid / refund
- [x] При `cancelled_by_gym`: причина + apology (FR-021) — подготовка к итерации 6
- [x] Кнопка «Отменить запись» → SCR-008
- [x] Кнопка «Изменить прокат» — заготовка (итерация 5)

### LOGIC-008 — Отмена с политикой

- [x] SCR-008 Cancellation Confirmation
- [x] UI по `cancellation_policy.warning_level`:
  - [x] `forbidden` — кнопка disabled + пояснение (FR-019, BR-012)
  - [x] `late_cancellation` — предупреждение, confirm (FR-018, BR-011)
  - [x] `none` — обычное подтверждение (FR-017, BR-010)
- [x] `DELETE /bookings/{id}` → 200 → back to SCR-006
- [x] 403 `CANCELLATION_FORBIDDEN` — snackbar
- [x] 409 — уже отменена

### Repositories / Use cases

- [x] `BookingRepository`: listBookings, getBooking, cancelBooking
- [x] `ListMyBookingsUseCase`, `GetBookingDetailUseCase`, `CancelBookingUseCase`
- [x] ViewModels для SCR-006, SCR-007, SCR-008

### Тесты

- [x] Unit: cancel button state по warning_level
- [x] Unit: forbidden не вызывает API

**✅ Готово, когда:** просмотр записей, отмена с тремя режимами политики.

**Backend:** [Итерация 4](./backend-implementation-plan.md#итерация-4--мои-записи-и-отмена).

---

## Итерация 5 — Изменение проката

### LOGIC-010 — Изменение проката в записи

- [x] SCR-007: режим редактирования проката (FR-028)
- [x] `GET /slots/{slotId}/rental-availability` с учётом текущей записи
- [x] UI выбора — переиспользовать компоненты из SCR-005 (LOGIC-007)
- [x] `PATCH /bookings/{id}/rental` → обновлённая сумма
- [x] 422 `RENTAL_UNAVAILABLE` → сообщение
- [x] 403 — запись не активна
- [x] Пересчёт и отображение нового `PaymentInfo`

### Тесты

- [x] Unit: diff rental lines
- [x] Integration: update → refresh booking detail

**✅ Готово, когда:** изменение проката на SCR-007 работает end-to-end.

**Backend:** [Итерация 5](./backend-implementation-plan.md#итерация-5--изменение-проката).

---

## Итерация 6 — Отмена скалодромом и альтернатива

### Реакция на отмену скалодромом (UC-004)

- [x] SCR-007: UI для `booking_status = cancelled_by_gym`
- [x] Показ `CancellationReason.title` + `apology_text` (FR-021)
- [x] Статус оплаты `refund` (FR-023, BR-021)
- [x] Баннер / CTA «Подобрать другой слот»

### LOGIC-009 — Альтернативный слот

- [x] SCR-009 Alternative Slot Offer (FR-022, BR-020)
- [x] `GET /slots/alternatives?cancelled_slot_id=&booking_id=`
- [x] `found=true` → карточка альтернативы + «Записаться»
- [x] `found=false` → «Ищите в расписании» + переход SCR-003
- [x] Tap «Записаться» → SCR-005 с новым slot_id
- [x] Блокировка повторной записи на отменённый слот (UI по `rebooking_forbidden`, BR-018)

### Deep link entry (частично)

- [x] Переход на SCR-009 из SCR-007 по push payload (без полного push пока)

### Тесты

- [x] Unit: alternatives found / not found UI
- [x] Unit: rebooking_forbidden hides book button

**✅ Готово, когда:** сценарий gym cancel → детали → альтернатива / расписание.

**Backend:** [Итерация 6](./backend-implementation-plan.md#итерация-6--отмена-скалодромом-и-альтернативный-слот).

---

## Итерация 7 — Профиль и лояльность

### SCR-010 — Profile Screen

- [x] `GET /clients/me` — ФИО, телефон, дата рождения
- [x] Бейдж «Постоянный клиент» при `is_loyal_client` (FR-027, BR-032)
- [x] Отображение `loyalty_discount`, `completed_visits_count`
- [x] Счётчики санкций (read-only, опционально для MVP)
- [x] Навигация → SCR-011
- [x] «Выйти» → clear token → SCR-002

### LOGIC-011 — Настройки уведомлений

- [x] SCR-011 Notification Settings (FR-032)
- [x] `GET /clients/me/notification-preferences`
- [x] Toggle: подтверждение записи (BR-029) — editable
- [x] Toggle: приглашение к оценке — editable (post-MVP push)
- [x] Напоминания — disabled, always on (BR-028)
- [x] Отмена скалодромом — disabled, always on (BR-028)
- [x] `PATCH` при изменении toggle

### Тесты

- [x] Unit: read-only toggles не отправляются в PATCH
- [x] UI: loyalty badge visibility

**✅ Готово, когда:** профиль + настройки push работают.

**Backend:** [Итерация 1](./backend-implementation-plan.md#итерация-1--клиент-и-авторизация) (preferences) + [Итерация 7](./backend-implementation-plan.md#итерация-7--push-токены-и-фоновые-задачи) worker (не блокирует UI).

---

## Итерация 8 — Push и маршрутизация

### LOGIC-012 — Регистрация push-токена

- [x] expect/actual: получение FCM (Android) / APNs (iOS) token
- [x] Запрос разрешения на уведомления
- [x] `PUT /devices/push-token` после auth (SCR-001, SCR-002)
- [x] Re-register при refresh token / app update

### LOGIC-013 — Deep link и push routing

- [x] SCR-014 Push Notification View
- [x] Парсинг payload: `{ type, booking_id, slot_id }`
- [x] Маршрутизация:
  - [x] `booking_confirmed` → SCR-006 / SCR-007
  - [x] `reminder` → SCR-007
  - [x] `gym_cancellation` → SCR-009 или SCR-007
  - [x] `rating_invitation` → SCR-012 (post-MVP stub)
- [x] Cold start из push → корректный back stack
- [x] Foreground: in-app banner / SCR-014

### Platform

- [x] Android: Firebase Messaging + notification channel
- [x] iOS: APNs + UNUserNotificationCenter delegate
- [x] Обработка tap на notification

### Тесты

- [x] Unit: payload → destination screen mapping
- [ ] Manual: все 4 типа push на staging

**✅ Готово, когда:** push доставляются, tap ведёт на нужный экран.

**Backend:** [Итерация 7](./backend-implementation-plan.md#итерация-7--push-токены-и-фоновые-задачи).

---

## Итерация 9 — Polish и hardening

### UX / NFR

- [x] NFR-001: быстрый доступ к расписанию и записям (lazy lists, cache)
- [x] NFR-003: единый каталог user-facing сообщений по error codes
- [x] NFR-007: оптимизация первого экрана (splash → schedule < 2s на staging)
- [x] Accessibility: размер шрифтов, contentDescription
- [x] Обработка потери сети на всех экранах

### Качество

- [x] Unit tests: все use cases MVP
- [x] UI tests (Android): register → book → cancel
- [x] Screenshot / preview tests ключевых экранов
- [x] ProGuard/R8 rules (Android), iOS release config

### Release

- [x] Env switcher (dev/staging/prod) — debug only
- [x] App version на SCR-001 (optional)
- [x] README: сборка Android/iOS, env vars
- [ ] TestFlight / internal APK на staging (ручная публикация)

**✅ MVP client freeze.**

---

## Post-MVP — Оценка инструктора

- [x] SCR-012 Rating Screen (черновик ТЗ)
- [x] LOGIC-014: `POST /ratings`, stars 1–5 (FR-029, BR-033)
- [x] Блокировка при `cancelled_by_gym` (FR-030, BR-034)
- [x] Окно 1–2 суток — UI + error `RATING_WINDOW_EXPIRED`
- [x] Push `rating_invitation` → SCR-012 (FR-031)
- [x] Отображение `average_rating` на SCR-003/004 (FR-004)

---

## Сводная таблица итераций

| # | Фокус | Экраны | Логики | Backend |
|---|--------|--------|--------|---------|
| 0 | Каркас CMP | shell + tabs | — | mock |
| 1 | Auth | SCR-001, SCR-002 | LOGIC-001, 002 | iter 1 |
| 2 | Расписание | SCR-003, 004, 013 | LOGIC-003, 004 | iter 2 |
| 3 | Запись | SCR-005, 015 | LOGIC-005, 006, 007 | iter 3 |
| 4 | Мои записи | SCR-006, 007, 008 | LOGIC-008 | iter 4 |
| 5 | Прокат | SCR-007 | LOGIC-010 | iter 5 |
| 6 | Gym cancel | SCR-007, 009 | LOGIC-009 | iter 6 |
| 7 | Профиль | SCR-010, 011 | LOGIC-011 | iter 1 |
| 8 | Push | SCR-014 | LOGIC-012, 013 | iter 7 |
| 9 | Hardening | все | — | staging |
| PM | Оценка | SCR-012 | LOGIC-014 | post-MVP |

---

## Матрица экранов MVP (15 шт.)

| ID | Экран | Итерация |
|----|-------|----------|
| SCR-001 | Splash | 1 |
| SCR-002 | Registration | 1 |
| SCR-003 | Schedule | 2 |
| SCR-004 | Slot Detail | 2 |
| SCR-005 | Booking | 3 |
| SCR-006 | My Bookings | 4 |
| SCR-007 | Booking Detail | 4 (+5, +6) |
| SCR-008 | Cancel Confirm | 4 |
| SCR-009 | Alternative Slot | 6 |
| SCR-010 | Profile | 7 |
| SCR-011 | Notification Settings | 7 |
| SCR-013 | Empty State | 2 |
| SCR-014 | Push View | 8 |
| SCR-015 | Risk Consent | 3 |
| SCR-012 | Rating | Post-MVP |

---

## Стратегия параллельной работы с backend

| Пока backend не готов | Действие на клиенте |
|-----------------------|---------------------|
| Итерация 0–1 | Mock server / JSON fixtures |
| Итерация 2+ | Переключение endpoint group на staging по мере готовности |
| Итерация 3 | Contract test: client DTO ↔ openapi.yaml |
| Итерация 8 | Staging + реальные push |

---

## Финальный чек-лист «клиент MVP готов»

- [x] 15 экранов реализованы по ТЗ (включая SCR-012)
- [x] 14 логик (LOGIC-001…014) работают
- [x] Все FR-001…FR-031 покрыты
- [x] JWT-сессия, secure storage, offline cache расписания
- [x] Push: confirm, reminder, gym cancel → правильная навигация
- [x] Error codes API → понятные русские сообщения
- [ ] Сборки Android + iOS на staging против реального backend
- [x] README с инструкцией запуска

---

## Связанные документы

- [План реализации backend](./backend-implementation-plan.md)
- [Mobile app spec](../01-analysis/5-mobile-app-spec/README.md)
- [Screen registry](../01-analysis/3-design-brief/screen-registry.md)
- [Logics index](../01-analysis/5-mobile-app-spec/09_Logics/_INDEX.md)
