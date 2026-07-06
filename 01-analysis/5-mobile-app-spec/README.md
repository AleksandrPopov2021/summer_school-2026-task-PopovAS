# Техническая спецификация мобильного приложения — Скалодром «Вертикаль»

**Версия:** 1.0.0  
**Платформа:** iOS / Android (mobile-first)  
**Источник API:** [openapi.yaml](../api/openapi.yaml)  
**Реестр экранов:** [screen-registry.md](../3-design-brief/screen-registry.md)

---

## Назначение документации

Данный каталог содержит технические задания (ТЗ) на мобильное клиентское приложение скалодрома «Вертикаль». Документация разбита по доменам: экраны (UI), логики (бизнес-поведение клиента) и шаблоны.

**Принципы:**
- API — единственный источник истины для данных (NFR-002)
- Бизнес-правила валидируются на бэкенде; UI отражает флаги и политики из ответов API
- Локальное хранение используется для сессии, кэша и черновиков записи

---

## Структура каталога

| Домен | Индекс | Описание | Статус |
|-------|--------|----------|--------|
| 01. Авторизация | [01_Authentication/_INDEX.md](01_Authentication/_INDEX.md) | Splash, регистрация | Актуален |
| 02. Расписание | [02_Schedule/_INDEX.md](02_Schedule/_INDEX.md) | Расписание, детали слота, empty state | Актуален |
| 03. Запись | [03_Booking/_INDEX.md](03_Booking/_INDEX.md) | Запись, согласие на риск, прокат | Актуален |
| 04. Мои записи | [04_My_Bookings/_INDEX.md](04_My_Bookings/_INDEX.md) | Список записей, отмена, альтернативы | Актуален |
| 05. Профиль | [05_Profile/_INDEX.md](05_Profile/_INDEX.md) | Профиль, лояльность, настройки | Актуален |
| 06. Уведомления | [06_Notifications/_INDEX.md](06_Notifications/_INDEX.md) | Просмотр push-уведомлений | Актуален |
| 07. Оценка | [07_Rating/_INDEX.md](07_Rating/_INDEX.md) | Оценка инструктора (Post-MVP) | Черновик |
| 09. Логики | [09_Logics/_INDEX.md](09_Logics/_INDEX.md) | Переиспользуемая бизнес-логика клиента | Актуален |

**Шаблоны:**
- [\_SCREEN_TEMPLATE.md](_SCREEN_TEMPLATE.md) — шаблон ТЗ экрана
- [\_LOGIC_TEMPLATE.md](_LOGIC_TEMPLATE.md) — шаблон ТЗ логики

---

## Реестр экранов (сводка)

| ID | Название | Домен | Приоритет | Статус ТЗ |
|----|----------|-------|-----------|-----------|
| SCR-001 | Splash Screen | 01. Авторизация | Critical | Актуален |
| SCR-002 | Registration Screen | 01. Авторизация | Critical | Актуален |
| SCR-003 | Schedule Screen | 02. Расписание | High | Актуален |
| SCR-004 | Slot Detail Screen | 02. Расписание | High | Актуален |
| SCR-005 | Booking Screen | 03. Запись | High | Актуален |
| SCR-006 | My Bookings Screen | 04. Мои записи | High | Актуален |
| SCR-007 | Booking Detail Screen | 04. Мои записи | High | Актуален |
| SCR-008 | Cancellation Confirmation Screen | 04. Мои записи | High | Актуален |
| SCR-009 | Alternative Slot Offer Screen | 04. Мои записи | Medium | Актуален |
| SCR-010 | Profile Screen | 05. Профиль | Medium | Актуален |
| SCR-011 | Notification Settings Screen | 05. Профиль | Medium | Актуален |
| SCR-012 | Rating Screen | 07. Оценка | Low (Post-MVP) | Черновик |
| SCR-013 | Empty State Screen | 02. Расписание | High | Актуален |
| SCR-014 | Push Notification View | 06. Уведомления | High | Актуален |
| SCR-015 | Consent Screen | 03. Запись | High | Актуален |

Полное описание экранов, переходов и бизнес-правил — в [screen-registry.md](../3-design-brief/screen-registry.md).

---

## Реестр логик (сводка)

| ID | Название | Приоритет | Статус |
|----|----------|-----------|--------|
| [LOGIC-001](09_Logics/LOGIC-001_Проверка-сессии-при-запуске.md) | Проверка сессии при запуске | Critical | Актуален |
| [LOGIC-002](09_Logics/LOGIC-002_Регистрация-клиента.md) | Регистрация клиента | Critical | Актуален |
| [LOGIC-003](09_Logics/LOGIC-003_Загрузка-расписания-слотов.md) | Загрузка расписания слотов | High | Актуален |
| [LOGIC-004](09_Logics/LOGIC-004_Отображение-доступности-слота.md) | Отображение доступности слота | High | Актуален |
| [LOGIC-005](09_Logics/LOGIC-005_Создание-записи-на-тренировку.md) | Создание записи на тренировку | Critical | Актуален |
| [LOGIC-006](09_Logics/LOGIC-006_Подтверждение-согласия-на-риск.md) | Подтверждение согласия на риск | High | Актуален |
| [LOGIC-007](09_Logics/LOGIC-007_Выбор-проката-и-расчёт-стоимости.md) | Выбор проката и расчёт стоимости | High | Актуален |
| [LOGIC-008](09_Logics/LOGIC-008_Отмена-записи-с-учётом-политики.md) | Отмена записи с учётом политики | High | Актуален |
| [LOGIC-009](09_Logics/LOGIC-009_Предложение-альтернативного-слота.md) | Предложение альтернативного слота | Medium | Актуален |
| [LOGIC-010](09_Logics/LOGIC-010_Изменение-проката-в-записи.md) | Изменение проката в записи | Medium | Актуален |
| [LOGIC-011](09_Logics/LOGIC-011_Управление-настройками-уведомлений.md) | Управление настройками уведомлений | Medium | Актуален |
| [LOGIC-012](09_Logics/LOGIC-012_Регистрация-push-токена.md) | Регистрация push-токена | High | Актуален |
| [LOGIC-013](09_Logics/LOGIC-013_Маршрутизация-deep-link-и-push.md) | Маршрутизация deep link и push | High | Актуален |
| [LOGIC-014](09_Logics/LOGIC-014_Оценка-инструктора.md) | Оценка инструктора (Post-MVP) | Low | Черновик |

Полный индекс — [09_Logics/_INDEX.md](09_Logics/_INDEX.md).

---

## Справочник API (operationId)

Все ссылки ведут на [openapi.yaml](../api/openapi.yaml).

| Группа | operationId | Метод | Endpoint | Используется в |
|--------|-------------|-------|----------|----------------|
| Clients | `registerClient` | POST | `/clients` | LOGIC-002 |
| Clients | `getCurrentClient` | GET | `/clients/me` | LOGIC-001, LOGIC-006 |
| Clients | `updateCurrentClient` | PATCH | `/clients/me` | LOGIC-006 |
| Clearances | `getClientClearances` | GET | `/clients/me/clearances` | LOGIC-004 |
| NotificationPreferences | `getNotificationPreferences` | GET | `/clients/me/notification-preferences` | LOGIC-011 |
| NotificationPreferences | `updateNotificationPreferences` | PATCH | `/clients/me/notification-preferences` | LOGIC-011 |
| Slots | `listSlots` | GET | `/slots` | LOGIC-003 |
| Slots | `getSlot` | GET | `/slots/{slotId}` | LOGIC-004, LOGIC-005 |
| Slots | `findAlternativeSlot` | GET | `/slots/alternatives` | LOGIC-009 |
| Slots | `getSlotRentalAvailability` | GET | `/slots/{slotId}/rental-availability` | LOGIC-007, LOGIC-010 |
| Bookings | `listBookings` | GET | `/bookings` | SCR-006 |
| Bookings | `createBooking` | POST | `/bookings` | LOGIC-005 |
| Bookings | `getBooking` | GET | `/bookings/{bookingId}` | LOGIC-008, LOGIC-009 |
| Bookings | `cancelBooking` | DELETE | `/bookings/{bookingId}` | LOGIC-008 |
| Bookings | `updateBookingRental` | PATCH | `/bookings/{bookingId}/rental` | LOGIC-007, LOGIC-010 |
| Reference | `listRentalEquipmentTypes` | GET | `/rental-equipment-types` | LOGIC-007 |
| Reference | `getSystemConfig` | GET | `/config` | LOGIC-001, LOGIC-003 |
| Devices | `registerPushToken` | PUT | `/devices/push-token` | LOGIC-012 |
| Ratings | `createInstructorRating` | POST | `/ratings` | LOGIC-014 (Post-MVP) |

---

## Локальное хранение (ключи)

| Ключ | Тип хранения | Описание | Логики |
|------|--------------|----------|--------|
| `access_token` | Защищённое хранилище (Keychain / EncryptedSharedPreferences) | JWT-токен сессии | LOGIC-001, LOGIC-002 |
| `cached_slots` | Локальный кэш | Кэш расписания слотов для офлайн-просмотра | LOGIC-003 |
| `cached_config` | Локальный кэш | Системные параметры (`SystemConfig`) | LOGIC-001, LOGIC-003 |
| `booking_draft` | Локальный кэш | Черновик записи (слот, прокат) до подтверждения | LOGIC-005, LOGIC-007 |

---

## Связанные документы

- [Функциональные требования (FR-001 … FR-032)](../2-requirements/functional-requirements.md)
- [Бизнес-требования (BR-001 … BR-035)](../2-requirements/business-requirements.md)
- [User Stories](../2-requirements/user-stories.md)
- [Use Cases](../2-requirements/use-cases.md)
- [Модели сущностей](../04-design/entity-models.md)
- [Диаграммы последовательностей](../04-design/sequence-diagrams.md)

---

## История релизов

| Релиз | Дата | Описание |
|-------|------|----------|
| 1.0.0 | 2026-07-04 | Первоначальная спецификация MVP: экраны авторизации и расписания, полный набор логик |
