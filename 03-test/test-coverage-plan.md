# Высокоуровневый план покрытия тестами — Скалодром «Вертикаль»

**Версия:** 1.0.0  
**Дата:** 2026-07-05  
**Область:** backend API (Go) + мобильный клиент (Compose Multiplatform, Android/iOS)

Документ описает **целевое** покрытие по уровням пирамиды тестирования и текущий статус автоматизации. Детальные тест-кейсы не входят в scope — см. FR/UC и спецификацию экранов/логик.

**Легенда статуса**

| Статус | Значение |
|--------|----------|
| ✅ | Автотесты реализованы и проходят в CI или локально |
| 🟡 | Частичное покрытие (unit без integration, happy path без негативных сценариев и т.п.) |
| ⬜ | Запланировано, автотестов нет |
| 🔵 | Ручное / exploratory-тестирование |
| ➖ | Post-MVP или вне scope MVP |

---

## 1. Пирамида тестирования

| Уровень | Назначение | Backend | Client (shared) | Client (UI) | E2E (client + backend) | Приоритет |
|---------|------------|---------|-----------------|-------------|------------------------|-----------|
| **Unit** | Изолированная бизнес-логика, валидация, расчёты | ✅ domain, application | ✅ domain, presentation mapping | — | — | Critical |
| **Integration** | HTTP handlers + PostgreSQL, репозитории | ✅ adapters/http | 🟡 data (mock engine) | — | ⬜ | Critical |
| **Contract** | Соответствие OpenAPI (схемы, коды, operationId) | 🟡 contract smoke | ⬜ | — | ⬜ | High |
| **Component / UI** | Экраны, навигация, состояния Loading/Error/Empty | — | — | 🟡 Android smoke | — | High |
| **E2E** | Сквозные пользовательские сценарии | — | — | 🟡 mock API | ⬜ staging | High |
| **Non-functional** | Производительность, локализация, offline | 🟡 concurrent booking | 🟡 offline session | 🔵 | ⬜ | Medium |

---

## 2. Покрытие по доменам приложения

| Домен | Ключевые артефакты | Unit | Integration | UI / E2E | Статус | Примечания |
|-------|-------------------|------|-------------|----------|--------|------------|
| **01. Авторизация** | SCR-001, SCR-002; LOGIC-001, LOGIC-002; FR-026 | ✅ phone, JWT, session use cases | ✅ register → me → consent | 🟡 регистрация в MvpFlowTest | 🟡 | iOS UI-тесты не добавлены |
| **02. Расписание** | SCR-003, SCR-004, SCR-013; LOGIC-003, LOGIC-004; FR-001–009 | ✅ slot availability, period parse | ✅ list/get slots, clearances | ⬜ empty state, фильтр даты | 🟡 | UI empty/cancelled slots — ручная проверка |
| **03. Запись** | SCR-005, SCR-015; LOGIC-005–007; FR-010–014 | ✅ pricing, draft, rental diff | ✅ create booking (все коды ошибок) | 🟡 book в MvpFlowTest | ✅ | Гонка за последнее место покрыта на backend |
| **04. Мои записи** | SCR-006–009; LOGIC-008–009; FR-016–022 | ✅ cancellation policy, alternatives | ✅ cancel, list, alternatives, gym cancel | 🟡 cancel в MvpFlowTest | 🟡 | SCR-009 alternative offer — без UI-теста |
| **05. Профиль** | SCR-010, SCR-011; LOGIC-011; FR-027, FR-032 | ✅ loyalty, notification prefs validation | 🟡 prefs — через client flow | ⬜ | 🟡 | PATCH notification-preferences — без отдельного integration |
| **06. Уведомления** | SCR-014; LOGIC-012–013; FR-015, FR-020, FR-024–025 | ✅ push routing, token validation | ✅ push token, push service | ⬜ | 🟡 | Реальная доставка FCM/APNs — manual/staging |
| **07. Оценка (Post-MVP)** | SCR-012; LOGIC-014; FR-029–031 | ✅ rating window, stars | ✅ create rating | ➖ | 🟡 | UI Post-MVP |
| **Справочники / config** | GET /config, /rental-equipment-types | ✅ parse config in domain | ✅ contract paths | ⬜ | ✅ | — |
| **Internal / worker** | gym cancel, complete bookings, loyalty | ✅ worker loyalty | ✅ gym cancel E2E | — | ✅ | Internal API — staging/manual |
| **Cross-cutting** | NFR-003 errors, NFR-005 RU | ✅ ErrorResponse mapping | ✅ contract integration | 🔵 | 🟡 | Локализация — snapshot/manual |

---

## 3. Покрытие функциональных требований (FR)

| ID | Требование (кратко) | Backend | Client unit | UI / E2E | Статус |
|----|---------------------|---------|-------------|----------|--------|
| FR-001 | Расписание на 7 дней | ✅ GET /slots | ✅ LoadSchedule | ⬜ | 🟡 |
| FR-002 | Фильтр по дате | ✅ query `date` | ✅ | ⬜ | 🟡 |
| FR-003 | Карточка слота (поля) | ✅ slot detail | ✅ mapping | ⬜ | 🟡 |
| FR-004 | Средняя оценка инструктора | ➖ | ➖ | ➖ | ➖ |
| FR-005 | Empty state | ✅ пустой список | ✅ | ⬜ | 🟡 |
| FR-006 | Отменённые слоты в списке | ✅ cancelled_by_gym | ✅ | ⬜ | 🟡 |
| FR-007 | Блокировка при 0 мест | ✅ availability | ✅ | ⬜ | ✅ |
| FR-008 | Cutoff 30 мин | ✅ | ✅ | ⬜ | ✅ |
| FR-009 | Допуск на трассы | ✅ clearances | ✅ | ⬜ | ✅ |
| FR-010 | Выбор снаряжения / прокат | ✅ rental lines | ✅ rental diff | 🟡 | 🟡 |
| FR-011 | Разбивка стоимости | ✅ pricing | ✅ CalculatePrice | ⬜ | ✅ |
| FR-012 | Согласие на риск | ✅ 403 без consent | ✅ | 🟡 consent screen | 🟡 |
| FR-013 | Создание записи через API | ✅ POST /bookings | ✅ repositories | 🟡 | ✅ |
| FR-014 | Отказ бронирования (409) | ✅ conflict response | ✅ CreateBookingConflict | ⬜ | ✅ |
| FR-015 | Push подтверждения записи | ✅ push service | ✅ routing | ⬜ | 🟡 |
| FR-016 | Список записей | ✅ GET /bookings | ✅ | 🟡 tab bookings | 🟡 |
| FR-017 | Отмена > 2 ч | ✅ policy none | ✅ | 🟡 | ✅ |
| FR-018 | Отмена 1–2 ч с предупреждением | ✅ late_cancellation | ✅ | ⬜ | 🟡 |
| FR-019 | Запрет отмены < 1 ч | ✅ 403 | ✅ | ⬜ | ✅ |
| FR-020 | Push отмены скалодромом | ✅ notify gym cancel | ✅ routing | ⬜ | 🟡 |
| FR-021 | Причина отмены скалодромом | ✅ cancellation_reason | ⬜ | ⬜ | 🟡 |
| FR-022 | Альтернативный слот | ✅ GET /alternatives | ✅ | ⬜ | 🟡 |
| FR-023 | Статус оплаты | ✅ payment in booking | ⬜ | ⬜ | 🟡 |
| FR-024 | Push за сутки | ✅ worker (log) | ⬜ | ⬜ | ⬜ |
| FR-025 | Push за N часов | ✅ worker (log) | ⬜ | ⬜ | ⬜ |
| FR-026 | Регистрация по телефону | ✅ POST /clients | ✅ validator | 🟡 | ✅ |
| FR-027 | Бейдж лояльности | ✅ loyalty threshold | ✅ ProfileLoyalty | ⬜ | 🟡 |
| FR-028 | Изменение проката | ✅ PATCH rental | ✅ integration | ⬜ | 🟡 |
| FR-029 | Оценка инструктора | ✅ POST /ratings | ✅ RatingLogic | ➖ | 🟡 |
| FR-030 | Блокировка оценки при gym cancel | ✅ | ✅ | ➖ | ✅ |
| FR-031 | Push-приглашение к оценке | ✅ push service | ✅ routing | ➖ | 🟡 |
| FR-032 | Настройки уведомлений | ✅ validation | ⬜ | ⬜ | 🟡 |

---

## 4. Покрытие use cases (UC)

| UC | Сценарий | Автотесты (основной уровень) | Негативные / alt paths | Статус |
|----|----------|------------------------------|------------------------|--------|
| UC-001 | Просмотр расписания | backend slot_integration; client SlotAvailability | empty date, offline cache | 🟡 |
| UC-002 | Запись на тренировку | booking_integration; MvpUseCases; MvpFlowTest | no spots, cutoff, clearance, 409 | ✅ |
| UC-003 | Отмена клиентом | cancellation domain; booking_integration | forbidden < 1h, late warning | 🟡 |
| UC-004 | Отмена скалодромом | gym cancel integration; push | rebooking_forbidden | ✅ |
| UC-005 | Push-напоминания | worker + push service unit | disabled prefs, timing | ⬜ |
| UC-006 | Регистрация | client_integration; CheckSession | 409 duplicate phone | ✅ |
| UC-007 | Статус лояльности | loyalty domain; push integration | below/at threshold | ✅ |
| UC-008 | Изменение проката | update rental integration | rental unavailable | 🟡 |
| UC-009 | Оценка инструктора | rating_integration | expired window, gym cancelled | 🟡 |
| UC-010 | Просмотр рейтинга | ➖ Post-MVP | — | ➖ |
| UC-011 | Настройки уведомлений | client validation unit | reject mandatory flags | 🟡 |
| UC-012 | Статус оплаты | payment in booking API | unpaid / refund display | 🟡 |

---

## 5. Покрытие API (OpenAPI operationId)

| Группа | operationId | Метод | Unit / domain | HTTP integration | Contract | Статус |
|--------|-------------|-------|---------------|------------------|----------|--------|
| Clients | registerClient | POST /clients | ✅ | ✅ | 🟡 | ✅ |
| Clients | getCurrentClient | GET /clients/me | — | ✅ | 🟡 | ✅ |
| Clients | updateCurrentClient | PATCH /clients/me | ✅ consent | ✅ | 🟡 | ✅ |
| Clearances | getClientClearances | GET …/clearances | ✅ | ✅ | 🟡 | ✅ |
| NotificationPreferences | getNotificationPreferences | GET | ✅ | 🟡 | 🟡 | 🟡 |
| NotificationPreferences | updateNotificationPreferences | PATCH | ✅ | ⬜ | 🟡 | 🟡 |
| Slots | listSlots | GET /slots | ✅ | ✅ | 🟡 | ✅ |
| Slots | getSlot | GET /slots/{id} | ✅ | ✅ | 🟡 | ✅ |
| Slots | findAlternativeSlot | GET /alternatives | ✅ | ✅ | 🟡 | ✅ |
| Slots | getSlotRentalAvailability | GET …/rental-availability | — | ✅ | 🟡 | ✅ |
| Bookings | listBookings | GET /bookings | — | ✅ | 🟡 | ✅ |
| Bookings | createBooking | POST /bookings | ✅ | ✅ (+ concurrency) | 🟡 | ✅ |
| Bookings | getBooking | GET /bookings/{id} | ✅ policy | ✅ | 🟡 | ✅ |
| Bookings | cancelBooking | DELETE | ✅ | ✅ | 🟡 | ✅ |
| Bookings | updateBookingRental | PATCH …/rental | — | ✅ | 🟡 | ✅ |
| Reference | listRentalEquipmentTypes | GET | — | 🟡 | 🟡 | 🟡 |
| Reference | getSystemConfig | GET /config | — | 🟡 | 🟡 | 🟡 |
| Devices | registerPushToken | PUT …/push-token | ✅ | ✅ | 🟡 | ✅ |
| Ratings | createInstructorRating | POST /ratings | ✅ | ✅ | 🟡 | 🟡 |
| Internal | cancelSlotByGym | POST internal | — | ✅ | ⬜ | 🟡 |

---

## 6. Покрытие клиентских логик (LOGIC)

| ID | Логика | Unit (shared) | Data layer | UI (Compose) | Статус |
|----|--------|---------------|------------|--------------|--------|
| LOGIC-001 | Проверка сессии | ✅ CheckSessionUseCase | ✅ AuthRepository | 🟡 splash flow | ✅ |
| LOGIC-002 | Регистрация | ✅ RegistrationValidator | ✅ | 🟡 SCR-002 | ✅ |
| LOGIC-003 | Загрузка расписания | ✅ MvpUseCases | ⬜ | ⬜ | 🟡 |
| LOGIC-004 | Доступность слота | ✅ SlotAvailability | — | ⬜ | 🟡 |
| LOGIC-005 | Создание записи | ✅ BookingDraft | ✅ conflict | 🟡 | ✅ |
| LOGIC-006 | Согласие на риск | ✅ | — | 🟡 | 🟡 |
| LOGIC-007 | Прокат и цена | ✅ CalculatePrice, RentalDiff | ✅ | ⬜ | 🟡 |
| LOGIC-008 | Отмена с политикой | ✅ BookingCancellation | — | 🟡 cancel | 🟡 |
| LOGIC-009 | Альтернативный слот | ✅ AlternativeSlotLogic | — | ⬜ | 🟡 |
| LOGIC-010 | Изменение проката | ✅ | ✅ UpdateRental | ⬜ | 🟡 |
| LOGIC-011 | Настройки уведомлений | ⬜ | ⬜ | ⬜ | ⬜ |
| LOGIC-012 | Push-токен | ⬜ | ⬜ | ⬜ | ⬜ |
| LOGIC-013 | Deep link / push routing | ✅ PushRouting | — | ⬜ | 🟡 |
| LOGIC-014 | Оценка инструктора | ✅ RatingLogic | — | ➖ | 🟡 |

---

## 7. Нефункциональные требования (NFR)

| ID | Требование | Тип теста | Статус | Комментарий |
|----|------------|-----------|--------|-------------|
| NFR-001 | Mobile-first UX | Manual / UX review | 🔵 | Чек-лист по SCR на реальном устройстве |
| NFR-002 | Актуальность данных API | Integration + E2E staging | ⬜ | Client против реального backend |
| NFR-003 | Понятные ошибки | Unit errors mapping | ✅ | Проверить тексты на RU в UI — 🔵 |
| NFR-004 | Обязательные push | Unit + API validation | 🟡 | Нельзя отключить reminders/gym_cancel |
| NFR-005 | Русский UI | Snapshot / manual | 🔵 | Строки в resources, без i18n-тестов |
| NFR-006 | Конfigurable N | Unit worker/loyalty | ✅ | system_config в seed |
| NFR-007 | Быстрый доступ к расписанию | Performance | ⬜ | KPI: cold start → schedule < 3s |
| NFR-008 | API as source of truth | Contract + integration | 🟡 | Расширить contract на все paths |
| NFR-009 | Обработка отказов API | Unit + integration | ✅ | 409/422/403 mapping |

---

## 8. CI/CD и окружения

| Компонент | Pipeline | Что запускается | Статус |
|-----------|----------|-----------------|--------|
| Backend | `.github/workflows/backend-ci.yml` | migrate → seed → `go test ./...`, golangci-lint | ✅ |
| Client unit | — | `:shared:allTests` | ⬜ CI не настроен |
| Client Android UI | — | `connectedDebugAndroidTest` | ⬜ CI не настроен |
| Contract (OpenAPI) | backend contract_test | smoke по MVP paths | 🟡 |
| Staging E2E | docker-compose.staging | client → staging API | ⬜ |

---

## 9. Приоритетный backlog автоматизации

| # | Задача | Уровень | Связь | Приоритет |
|---|--------|---------|-------|-----------|
| 1 | Client CI: unit-тесты shared на каждый PR | CI | все LOGIC | High |
| 2 | Integration: PATCH notification-preferences | HTTP | FR-032, LOGIC-011 | High |
| 3 | E2E staging: register → book → cancel (real API) | E2E | UC-002, UC-003 | High |
| 4 | UI: SCR-013 empty state, SCR-006 list states | Compose | FR-005, FR-016 | Medium |
| 5 | Worker: напоминания FR-024/025 (timing mocks) | Integration | UC-005 | Medium |
| 6 | Contract: полное покрытие OpenAPI responses | Contract | NFR-008 | Medium |
| 7 | iOS UI smoke (XCTest / KMP UI) | UI | NFR-001 | Medium |
| 8 | Performance: 50+ concurrent bookings (уже есть) → baseline в CI | Load | FR-013 | Low |
| 9 | Post-MVP: SCR-012 rating flow UI | UI | FR-029–031 | Low |

---

## 10. Ручное тестирование (exploratory)

Рекомендуемые сессии перед релизом MVP (не дублируют автотесты):

| Сессия | Фокус | Чек-лист |
|--------|-------|----------|
| **Smoke** | Happy path на staging | UC-006 → UC-001 → UC-002 → UC-003 |
| **Offline** | LOGIC-001, LOGIC-003 | Нет сети на splash; кэш расписания |
| **Push** | SCR-014, FR-015/020 | Реальное устройство, tap → deep link |
| **Accessibility** | NFR-001 | TalkBack, контраст, размер шрифта |
| **Regression** | Отмена gym + alternative | UC-004, FR-022, SCR-009 |

---

## История изменений

| Версия | Дата | Описание |
|--------|------|----------|
| 1.0.0 | 2026-07-05 | Первоначальный высокоуровневый план покрытия |
