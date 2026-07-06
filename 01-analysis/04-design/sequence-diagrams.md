# Sequence-диаграммы — Скалодром «Вертикаль»

> Взаимодействие участников в ключевых сценариях клиентского приложения.

**Участники:**
- **Клиент** — пользователь мобильного приложения
- **Приложение** — клиентское мобильное приложение (UI + локальная логика)
- **API** — бэкенд скалодрома (источник истины)
- **Push** — сервис push-уведомлений

---

## 1. UC-002: Запись на тренировку (основной сценарий)

```mermaid
sequenceDiagram
    autonumber
    actor Клиент
    participant App as Приложение
    participant API as Бэкенд API
    participant Push as Push-сервис

    Клиент->>App: Открыть расписание
    App->>API: GET /slots?from=&to= (7 дней)
    API-->>App: Список слотов (места, зона, инструктор, прокатный фонд)
    App-->>Клиент: Отобразить слоты на выбранную дату

    Клиент->>App: Выбрать слот
    App->>App: Проверить: free_spots > 0, до начала ≥ 30 мин

    alt Опытная тренировка (rope_routes)
        App->>API: GET /clients/me/clearances
        API-->>App: Статус допуска
        alt Допуск отсутствует
            App-->>Клиент: Запись недоступна (сообщение)
        end
    end

    Клиент->>App: Выбрать снаряжение (своё / прокат по позициям)
    App->>App: Рассчитать разбивку: тренировка + прокат + итого
    App-->>Клиент: Показать стоимость

    alt Первая запись клиента
        App-->>Клиент: Запросить согласие на риск
        Клиент->>App: Подтвердить согласие
        App->>API: PATCH /clients/me { risk_consent_accepted: true }
        API-->>App: OK
    end

    Клиент->>App: Подтвердить запись
    App->>API: POST /bookings { slot_id, uses_own_equipment, rental_lines[] }

    alt Места заняты / конфликт
        API-->>App: 409 Conflict + актуальный слот
        App->>API: GET /slots/{id}
        API-->>App: Обновлённый слот (free_spots)
        App-->>Клиент: Сообщение + актуальное состояние слота
    else Бронирование успешно
        API-->>App: 201 Created + Booking + PaymentInfo
        App-->>Клиент: Экран подтверждения записи

        opt Подтверждение записи не отключено
            API->>Push: Отправить push «Запись подтверждена»
            Push-->>Клиент: Push-уведомление
        end
    end
```

**Связанные требования:** UC-002, FR-008–FR-014, BR-002–BR-008, US-003–US-008.

---

## 2. UC-003: Отмена записи клиентом

```mermaid
sequenceDiagram
    autonumber
    actor Клиент
    participant App as Приложение
    participant API as Бэкенд API

    Клиент->>App: Открыть «Мои записи»
    App->>API: GET /bookings?status=booked
    API-->>App: Список активных записей
    App-->>Клиент: Отобразить записи

    Клиент->>App: Выбрать запись → Отменить
    App->>App: Вычислить время до начала слота

    alt Менее 1 часа до начала
        App-->>Клиент: Кнопка отмены заблокирована (BR-012)
    else От 1 до 2 часов
        App-->>Клиент: Предупреждение о поздней отмене
        Клиент->>App: Подтвердить отмену
        App->>API: DELETE /bookings/{id}
        API-->>App: 200 OK, booking_status=cancelled_by_client
        App-->>Клиент: Запись отменена, место освобождено
    else Более 2 часов
        Клиент->>App: Подтвердить отмену
        App->>API: DELETE /bookings/{id}
        API-->>App: 200 OK, booking_status=cancelled_by_client
        App-->>Клиент: Запись отменена
    end
```

**Связанные требования:** UC-003, FR-017–FR-019, BR-010–BR-013, US-010–US-011.

---

## 3. UC-004: Отмена тренировки скалодромом (реакция приложения)

> Инициатор отмены — администратор в существующей инфраструктуре; приложение **только потребляет** обновлённые данные и push.

```mermaid
sequenceDiagram
    autonumber
    actor Admin as Администратор
    participant Infra as Инфраструктура скалодрома
    participant API as Бэкенд API
    participant Push as Push-сервис
    participant App as Приложение
    actor Клиент

    Admin->>Infra: Отменить слот (причина из справочника)
    Infra->>API: Обновить слот и записи
    API->>API: slot_status = cancelled_by_gym
    API->>API: booking_status = cancelled_by_gym для всех записей
    API->>API: payment_status = refund (если было paid)

    API->>Push: Push «Тренировка отменена» (обязательный)
    Push-->>App: Доставить уведомление
    App-->>Клиент: Показать push + детали отмены

    Клиент->>App: Открыть детали записи
    App->>API: GET /bookings/{id}
    API-->>App: Запись + CancellationReason + PaymentInfo
    App-->>Клиент: Причина, извинения, статус возврата

    App->>API: GET /slots/alternatives?zone=&instructor=&...
    API-->>App: Ближайший свободный слот (или пусто)
    alt Альтернатива найдена
        App-->>Клиент: Предложить альтернативный слот (BR-020)
    else Альтернативы нет
        App-->>Клиент: Поиск замены в расписании (US-024)
    end

    Note over App,Клиент: Повторная запись на тот же слот запрещена (BR-018)
```

**Связанные требования:** UC-004, FR-020–FR-022, BR-016–BR-021, US-012–US-014, US-025.

---

## 4. UC-008: Изменение проката после записи

```mermaid
sequenceDiagram
    autonumber
    actor Клиент
    participant App as Приложение
    participant API as Бэкенд API

    Клиент->>App: Открыть активную запись
    App->>API: GET /bookings/{id}
    API-->>App: Booking + rental_lines + PaymentInfo

    Клиент->>App: Изменить прокат
    App->>API: GET /slots/{slot_id}/rental-availability
    API-->>App: SlotRentalAvailability по позициям

    alt Нужные позиции недоступны
        App-->>Клиент: Изменение невозможно
    else Позиции доступны
        Клиент->>App: Выбрать новые позиции проката
        App->>API: PATCH /bookings/{id}/rental { rental_lines[] }
        API-->>App: Обновлённая запись + новая разбивка стоимости
        App-->>Клиент: Прокат и сумма обновлены
    end
```

**Связанные требования:** UC-008, FR-028, US-019.

---

## 5. UC-005 + UC-006: Регистрация и напоминания

### 5.1. Регистрация (UC-006)

```mermaid
sequenceDiagram
    autonumber
    actor User as Новый пользователь
    participant App as Приложение
    participant API as Бэкенд API

    User->>App: Первый запуск приложения
    App-->>User: Форма регистрации
    User->>App: Телефон, ФИО, дата рождения
    App->>API: POST /clients { phone, full_name, birth_date }
    API-->>App: 201 Created + Client
    App->>API: GET /clients/me/notification-preferences
    API-->>App: Настройки по умолчанию
    App-->>User: Доступ к расписанию
```

### 5.2. Push-напоминания (UC-005)

```mermaid
sequenceDiagram
    autonumber
    participant API as Бэкенд API
    participant Push as Push-сервис
    participant App as Приложение
    actor Клиент

    Note over API: За сутки до starts_at
    API->>Push: Напоминание (обязательное)
    Push-->>App: Push за 24 ч
    App-->>Клиент: Уведомление

    Note over API: За N часов в день тренировки (из SystemConfig)
    API->>Push: Напоминание (обязательное)
    Push-->>App: Push за N ч
    App-->>Клиент: Уведомление
```

**Связанные требования:** UC-005, UC-006, FR-024–FR-026, BR-027–BR-028, US-016–US-017.

---

## 6. UC-009: Оценка инструктора (Post-MVP)

```mermaid
sequenceDiagram
    autonumber
    actor Клиент
    participant App as Приложение
    participant API as Бэкенд API
    participant Push as Push-сервис

    API->>Push: Приглашение оценить (если включено)
    Push-->>App: Push после завершения тренировки
    Клиент->>App: Открыть форму оценки

    App->>API: GET /bookings/{id}
    API-->>App: booking_status=completed

    alt Отменена скалодромом или окно истекло
        App-->>Клиент: Оценка недоступна (BR-034)
    else Доступна оценка
        Клиент->>App: Выставить звёзды 1–5
        App->>API: POST /ratings { booking_id, stars }
        API-->>App: 201 Created
        API->>API: Пересчитать average_rating инструктора
        App-->>Клиент: Спасибо за оценку
    end
```

**Связанные требования:** UC-009, FR-029–FR-031, BR-033–BR-035, US-020.

---

## 7. Карта диаграмм к use cases

| Диаграмма | Use Case | Приоритет |
|-----------|----------|-----------|
| §1 Запись на тренировку | UC-002 | MVP |
| §2 Отмена клиентом | UC-003 | MVP |
| §3 Отмена скалодромом | UC-004 | MVP |
| §4 Изменение проката | UC-008 | MVP |
| §5.1 Регистрация | UC-006 | MVP |
| §5.2 Напоминания | UC-005 | MVP |
| §6 Оценка инструктора | UC-009 | Post-MVP |
