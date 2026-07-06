# Backend — Скалодром «Вертикаль» (MVP)

Go API (hexagonal architecture). Контракт: [`api/openapi.yaml`](api/openapi.yaml).

## За 5 минут

```bash
cd backend
cp .env.example .env          # 1. конфиг
make docker-up                # 2. PostgreSQL
make migrate                  # 3. схема БД
make seed                     # 4. справочники + слоты + тестовый клиент
make run                      # 5. API → http://localhost:8080/v1
```

В другом терминале (опционально):

```bash
make worker                   # push-напоминания, завершение записей, лояльность
```

Проверка:

```bash
curl http://localhost:8080/healthz
curl http://localhost:8080/v1/config
```

## Требования

- Go 1.23+
- Docker (PostgreSQL; для staging — полный стек)
- Make (опционально)

## MVP API (18 эндпоинтов)

| Метод | Путь | Auth |
|-------|------|------|
| GET | `/healthz` | — |
| GET | `/v1/config` | — |
| GET | `/v1/rental-equipment-types` | — |
| POST | `/v1/clients` | — |
| GET | `/v1/clients/me` | Bearer |
| PATCH | `/v1/clients/me` | Bearer |
| GET | `/v1/clients/me/clearances` | Bearer |
| GET | `/v1/clients/me/notification-preferences` | Bearer |
| PATCH | `/v1/clients/me/notification-preferences` | Bearer |
| GET | `/v1/slots` | optional Bearer |
| GET | `/v1/slots/{slotId}` | optional Bearer |
| GET | `/v1/slots/alternatives` | Bearer |
| GET | `/v1/slots/{slotId}/rental-availability` | Bearer |
| GET | `/v1/bookings` | Bearer |
| POST | `/v1/bookings` | Bearer |
| GET | `/v1/bookings/{bookingId}` | Bearer |
| DELETE | `/v1/bookings/{bookingId}` | Bearer |
| PATCH | `/v1/bookings/{bookingId}/rental` | Bearer |
| PUT | `/v1/devices/push-token` | Bearer |

Internal (не в клиентском OpenAPI): `POST /internal/slots/{slotId}/cancel-by-gym` (заголовок `X-API-Key`).

## Post-MVP — оценка инструктора (итерация 9)

| Метод | Путь | Auth | Описание |
|-------|------|------|----------|
| POST | `/v1/ratings` | Bearer | Оценка 1–5 звёзд в течение 48 ч после окончания тренировки |

Ошибки: `RATING_NOT_ALLOWED_GYM_CANCELLED`, `RATING_WINDOW_EXPIRED`, `RATING_ALREADY_SUBMITTED` (409).  
После `CompleteBookings` worker шлёт push `rating_invitation`, если включено в настройках.

## Для mobile-команды

### Base URL

| Окружение | URL |
|-----------|-----|
| Local dev | `http://localhost:8080/v1` |
| Staging (docker) | `http://localhost:8080/v1` после `make staging-up` |

### Получение токена

```bash
curl -s -X POST http://localhost:8080/v1/clients \
  -H "Content-Type: application/json" \
  -d '{"phone":"+79001234567","full_name":"Dev User","birth_date":"1995-01-01"}'
```

Ответ: `access_token` (JWT Bearer).

### Seed-данные для ручного теста

После `make seed`:

| Параметр | Значение |
|----------|----------|
| Телефон тестового клиента | `+79009999999` |
| Client ID | `10000000-0000-4000-8000-000000000001` |
| Допуск на трассы | есть (instructor `c0000000-...000001`) |
| `risk_consent_accepted` | `true` |
| Internal API key | `dev-internal-key` (см. `.env`) |

> Для JWT зарегистрируйте новый телефон через `POST /clients` или используйте токен из ответа регистрации.

### Системные параметры (seed)

- `booking_cutoff_minutes`: 30
- `cancellation_forbidden_minutes`: 60
- `reminder_hours_before`: 3
- `visits_for_loyalty`: 10
- `violations_for_sanctions`: 3

## Санкции (BR-014, BR-015)

`POST /bookings` возвращает **403** `BOOKING_FORBIDDEN_SANCTIONS`, если  
`late_cancellation_count + no_show_count >= violations_for_sanctions`.

## Команды

| Команда | Описание |
|---------|----------|
| `make setup` | docker-up + migrate + seed |
| `make run` | HTTP API |
| `make worker` | Фоновый worker |
| `make test` | Unit + integration тесты |
| `make test-integration` | Только integration |
| `make test-load` | 50 concurrent POST /bookings |
| `make lint` | golangci-lint |
| `make migrate` | goose up |
| `make seed` | Сид данных |
| `make generate` | sqlc generate |
| `make staging-up` | Postgres + migrate + seed + API + worker |
| `make staging-down` | Остановить staging |

## Staging (Docker)

```bash
make staging-up
# API: http://localhost:8080
# Postgres: localhost:5432 (vertical/vertical)
```

Сервисы: `postgres`, `migrate`, `api`, `worker`.  
Переменные staging: `JWT_SECRET=staging-change-me`, `INTERNAL_API_KEY=staging-internal-key`.

## CI

GitHub Actions: [`.github/workflows/backend-ci.yml`](../.github/workflows/backend-ci.yml)

- migrate на пустой PostgreSQL
- seed
- `go test ./...`
- golangci-lint

## Структура

```
backend/
├── cmd/api/              # HTTP-сервер
├── cmd/worker/           # Фоновые задачи
├── cmd/seed/             # Сид данных
├── internal/
│   ├── domain/
│   ├── application/
│   ├── ports/
│   └── adapters/
├── migrations/
├── sql/
├── api/openapi.yaml
├── Dockerfile
└── docker-compose.staging.yml
```
