# Маршрутизация deep link и push

**ID:** LOGIC-013  
**Тип:** Логика  
**Домен:** 09. Логики  
**Приоритет:** High  
**Статус:** Актуален  
**Функциональные блоки:** FB-NOTIF-003, FB-APP-003

---

## История изменений

| Релиз | ТЗ | Описание изменений |
|-------|-----|-------------------|
| 1.0.0 | [LOGIC-013](LOGIC-013_Маршрутизация-deep-link-и-push.md) | Первоначальная документация |

---

## Входные данные

| Название | Тип | Возможные значения | Описание |
|----------|-----|-------------------|----------|
| `payload.type` | Push / deep link | см. таблицу типов | Тип уведомления |
| `payload.booking_id` | Push / deep link | uuid | ID записи |
| `payload.slot_id` | Push / deep link | uuid | ID слота |
| `access_token` | Защищённое хранилище | JWT | Для защищённых маршрутов |

---

## Обзор

Глобальная логика разбора payload push-уведомлений и deep link URL, маршрутизации на целевые экраны и сохранения контекста уведомления. Работает совместно с LOGIC-001 (сессия) и LOGIC-009 (альтернативы).

### User Story

> Как клиент, я хочу открывать нужный экран по тапу на уведомление,
> чтобы сразу увидеть детали записи или предложенную альтернативу.

### Бизнес-ценность

- Конверсия из push в целевое действие (BR-026)
- Быстрый доступ к отменённым записям и альтернативам (BR-017, BR-020)
- Единая схема навигации для push и deep link

---

## Точки применения

| Экран/Компонент | Элемент/Триггер | Условие |
|-----------------|-----------------|---------|
| `DeepLinkRouter` | Открытие URL `vertical://...` | Cold/warm start |
| `PushNotificationHandler` | Тап на системное уведомление | Foreground/background/killed |
| SCR-014 Push Notification View | Промежуточный экран контекста | Опционально перед целевым экраном |
| [LOGIC-001](LOGIC-001_Проверка-сессии-при-запуске.md) | Перед маршрутизацией | Проверка сессии |

---

## Флоу

```mermaid
flowchart TD
    Start([Тап push / deep link]) --> Parse[Разбор payload / URL]
    
    Parse --> CheckAuth{access_token есть?}
    CheckAuth -->|Нет| SaveIntent[Сохранить pending_navigation]
    SaveIntent --> SCR002[SCR-002 регистрация]
    SCR002 --> AfterAuth[После авторизации — resume pending]
    AfterAuth --> Route
    CheckAuth -->|Да| Route{payload.type?}
    
    Route -->|booking_confirmed| NavBookings[SCR-006 или SCR-007 booking_id]
    Route -->|reminder| NavDetail[SCR-007 booking_id]
    Route -->|gym_cancellation| NavAlt[SCR-009 или SCR-007 + LOGIC-009]
    Route -->|rating_invitation| NavRating[SCR-012 Post-MVP]
    
    NavBookings --> End([Конец])
    NavDetail --> End
    NavAlt --> End
    NavRating --> End
    
    Parse --> DeepLink{Deep link path?}
    DeepLink -->|/schedule| SCR003[SCR-003]
    DeepLink -->|/bookings/{id}| SCR007[SCR-007]
    DeepLink -->|/bookings/{id}/alternative| SCR009[SCR-009]
    DeepLink -->|/ratings/{booking_id}| SCR012[SCR-012 Post-MVP]
    SCR003 --> End
    SCR007 --> End
    SCR009 --> End
    SCR012 --> End
```

---

## Описание логики

### Шаг 1: Формат push payload

Ожидаемая структура data-payload (FCM/APNs custom data):

| `type` | Обязательные поля | Целевой экран | FR |
|--------|-------------------|---------------|-----|
| `booking_confirmed` | `booking_id` | SCR-006 / SCR-007 | FR-015 |
| `reminder` | `booking_id`, опционально `reminder_kind` (`day_before`, `hours_before`) | SCR-007 | FR-024, FR-025 |
| `gym_cancellation` | `booking_id`, `slot_id`, опционально `cancellation_reason_code` | SCR-009 → LOGIC-009 | FR-020, FR-021 |
| `rating_invitation` | `booking_id` | SCR-012 (Post-MVP) | FR-031 |

### Шаг 2: Схема deep link

Базовый scheme: `vertical://`

| URL | Параметры | Экран |
|-----|-----------|-------|
| `vertical://schedule` | — | SCR-003 |
| `vertical://schedule?date=YYYY-MM-DD` | `date` | SCR-003 с датой |
| `vertical://bookings/{booking_id}` | `booking_id` | SCR-007 |
| `vertical://bookings/{booking_id}/alternative` | `booking_id` | SCR-009 |
| `vertical://ratings/{booking_id}` | `booking_id` | SCR-012 (Post-MVP) |

HTTPS App Links (опционально): `https://vertical-climbing.ru/app/...` — редирект в тот же роутер.

### Шаг 3: Проверка авторизации

Маршруты `/bookings/*`, `/ratings/*` требуют `access_token`. При отсутствии:
1. Сохранить `pending_navigation` в памяти
2. SCR-002 → после LOGIC-002 выполнить отложенную навигацию

Расписание (`/schedule`) доступно без авторизации для просмотра.

### Шаг 4: Обработка по типам

**booking_confirmed:** открыть SCR-007 с `booking_id` или SCR-014 с CTA «Посмотреть в моих записях» → SCR-006.

**reminder:** SCR-007 с деталями записи (дата, адрес, зона). Заголовок SCR-014: «Напоминание о тренировке».

**gym_cancellation:** SCR-009 с вызовом LOGIC-009 (`cancelled_slot_id` из payload, `booking_id`). Показать причину отмены. CTA «Посмотреть альтернативы».

**rating_invitation (Post-MVP):** SCR-012 с `booking_id`. Если LOGIC-014 вернёт недоступность — SCR-006 с сообщением.

### Шаг 5: Cold start

При запуске из killed state:
1. LOGIC-001 (сессия)
2. Парсинг initial notification / deep link intent
3. Маршрутизация после готовности навигатора

### Шаг 6: Дедупликация

Повторный тап на то же уведомление не создаёт дубликатов экранов в стеке — `singleTop` / replace navigation.

---

## API запросы

> Прямых API-запросов в логике маршрутизации нет. Целевые экраны загружают данные самостоятельно:

| Экран | operationId | Назначение |
|-------|-------------|------------|
| SCR-007 | `getBooking` | Детали записи |
| SCR-009 | `findAlternativeSlot` | LOGIC-009 |
| SCR-012 | — | LOGIC-014: `createInstructorRating` |

---

## Локальное хранение

| Ключ | Тип хранения | Описание |
|------|--------------|----------|
| `access_token` | Защищённое хранилище | Проверка перед маршрутом |
| `pending_navigation` | Память сессии (не persistent) | Отложенный deep link до авторизации |

---

## Связанные требования

### Функциональные (FR)

| ID | Название | Приоритет |
|----|----------|-----------|
| FR-015 | Push о подтверждении записи | High |
| FR-020 | Push при отмене скалодромом | High |
| FR-021 | Причина отмены скалодромом | High |
| FR-024 | Push-напоминание за сутки | High |
| FR-025 | Push-напоминание за N часов | High |
| FR-031 | Push-приглашение к оценке | Low (Post-MVP) |
| FR-022 | Предложение альтернативного слота | Medium |

### Бизнес-правила (BR)

| ID | Название |
|----|----------|
| BR-026 | Push-уведомления в MVP |
| BR-017 | Обязательный push при отмене скалодромом |
| BR-027 | Напоминания за сутки и за N часов |
| BR-020 | Альтернативный слот при отмене |

---

## Критерии приёмки

| ID | Критерий |
|----|----------|
| AC-001 | **Дано** push `type=booking_confirmed`, **Когда** пользователь тапает уведомление, **Тогда** открывается SCR-007 с `booking_id` |
| AC-002 | **Дано** push `type=reminder`, **Когда** пользователь тапает уведомление, **Тогда** открывается SCR-007 с деталями тренировки |
| AC-003 | **Дано** push `type=gym_cancellation`, **Когда** пользователь тапает «Посмотреть альтернативы», **Тогда** открывается SCR-009 и вызывается LOGIC-009 |
| AC-004 | **Дано** deep link `vertical://bookings/{id}`, **Когда** пользователь авторизован, **Тогда** открывается SCR-007 |
| AC-005 | **Дано** deep link на защищённый маршрут без токена, **Когда** ссылка открыта, **Тогда** SCR-002, после регистрации — целевой экран |
| AC-006 | **Дано** push `type=rating_invitation` (Post-MVP), **Когда** пользователь тапает уведомление, **Тогда** открывается SCR-012 |
| AC-007 | **Дано** приложение в killed state, **Когда** открытие из push, **Тогда** сначала LOGIC-001, затем маршрутизация |

---

## Обработка ошибок

| Тип ошибки | Контекст | Действие |
|------------|----------|----------|
| Неизвестный `type` | Push payload | SCR-003, логирование |
| Невалидный UUID в URL | Deep link | SCR-003 + снек |
| `getBooking` 404 | SCR-007 из push | SCR-006 + снек «Запись не найдена» |
| Неавторизован | Защищённый маршрут | pending_navigation → SCR-002 |

---

## Примеры payload

### booking_confirmed

```json
{
  "type": "booking_confirmed",
  "booking_id": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
}
```

### reminder

```json
{
  "type": "reminder",
  "booking_id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "reminder_kind": "day_before"
}
```

### gym_cancellation

```json
{
  "type": "gym_cancellation",
  "booking_id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
  "slot_id": "7c9e6679-7425-40de-944b-e07fc1f90ae7",
  "cancellation_reason_code": "instructor_unavailable"
}
```

### rating_invitation

```json
{
  "type": "rating_invitation",
  "booking_id": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
}
```
