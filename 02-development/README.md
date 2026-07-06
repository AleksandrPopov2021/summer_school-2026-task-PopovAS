# Этап разработки — Скалодром «Вертикаль»

Артефакты планирования и результаты реализации **MVP мобильного приложения клиента** и **REST API бэкенда** для самостоятельной записи на групповые тренировки. Разработка велась итерациями по планам в этом каталоге; исходные требования и ТЗ — в [`01-analysis/`](../01-analysis/).

## Цель этапа

Перевести результаты анализа в работающий продукт: спроектировать итеративный план, реализовать API и мобильный клиент по контракту OpenAPI, обеспечить воспроизводимую сборку, тесты и staging-окружение.

## Планирование

| Документ | Содержание |
|----------|------------|
| [`backend-implementation-plan.md`](backend-implementation-plan.md) | 9 итераций backend (каркас → auth → слоты → бронирование → отмена → прокат → gym cancel → push/worker → hardening) + Post-MVP (оценка инструктора) |
| [`client-implementation-plan.md`](client-implementation-plan.md) | 9 итераций клиента (CMP/KMP) с привязкой к экранам SCR-001…015 и логикам LOGIC-001…014; стратегия параллельной работы с mock API |

Формат планов: **итерация → чек-лист → критерий «готово»**. Клиентские итерации синхронизированы с готовностью соответствующих групп endpoint'ов backend.

## Произведённые работы

### Backend ([`backend/`](../backend/))

Реализован Go API в **hexagonal architecture** (Ports & Adapters):

- **Стек:** Go, chi, oapi-codegen, PostgreSQL, sqlc, goose, JWT
- **18 MVP endpoint'ов** по [`01-analysis/api/openapi.yaml`](../01-analysis/api/openapi.yaml): регистрация и профиль, расписание, бронирование, отмена, прокат, push-токены, справочники
- **Post-MVP:** `POST /v1/ratings` — оценка инструктора (1–5 звёзд, окно 48 ч)
- **Internal API:** `POST /internal/slots/{slotId}/cancel-by-gym` (отмена скалодромом для тестов и staging)
- **Доменная логика:** `BookingAvailability`, `CancellationPolicy`, расчёт стоимости с лояльностью, атомарное бронирование (`SELECT FOR UPDATE`), санкции (BR-014, BR-015)
- **Worker** (`cmd/worker`): напоминания, завершение записей, пересчёт лояльности, push-события
- **Инфраструктура:** Docker Compose (dev + staging), seed-данные, Makefile, GitHub Actions CI (migrate → seed → test → lint)

Подробности запуска и API: [`backend/README.md`](../backend/README.md).

### Mobile client ([`client/`](../client/))

Реализовано приложение на **Kotlin Multiplatform + Compose Multiplatform** (Android + iOS):

- **Стек:** Decompose, Koin, Ktor, kotlinx.serialization, Clean Architecture + MVI
- **15 экранов** (SCR-001…015): авторизация, расписание, запись, согласие на риск, мои записи, отмена, альтернативный слот, профиль, настройки push, оценка инструктора
- **14 логик клиента** (LOGIC-001…014): сессия, регистрация, offline-кэш, бронирование, политика отмены, push-маршрутизация и др.
- **Mock API** для автономной разработки; переключение dev/staging/prod в debug-сборке
- **Push:** FCM (Android), APNs (iOS), deep link по payload `{ type, booking_id, slot_id }`
- **Тесты:** unit-тесты use cases в `shared`, Android UI smoke (register → book → cancel)

Подробности сборки: [`client/README.md`](../client/README.md).

## Краткая структура проекта

```
summer_school-2026-task-PopovAS/
├── 01-analysis/              # Анализ: требования, дизайн, OpenAPI, ТЗ экранов
├── 02-development/           # Планы реализации (этот каталог)
│   ├── backend-implementation-plan.md
│   ├── client-implementation-plan.md
│   └── README.md
├── backend/                  # Go REST API + worker + миграции
│   ├── cmd/api/              # HTTP-сервер
│   ├── cmd/worker/           # Фоновые задачи (push, loyalty, complete)
│   ├── cmd/seed/             # Seed справочников и слотов
│   ├── internal/
│   │   ├── domain/           # Бизнес-правила и модели
│   │   ├── application/      # Use cases / сервисы
│   │   ├── ports/            # Интерфейсы репозиториев
│   │   └── adapters/         # HTTP, PostgreSQL, push, auth
│   ├── migrations/           # goose-миграции (17 таблиц)
│   ├── sql/                  # sqlc-схема и запросы
│   └── api/openapi.yaml      # Контракт API (копия из analysis)
├── client/                   # KMP/CMP мобильное приложение
│   ├── shared/               # domain / data / presentation / di
│   ├── composeApp/           # UI, навигация (Decompose), точки входа
│   └── iosApp/               # Xcode-обёртка для iOS
└── 03-test/                  # План покрытия тестами, реестр автотестов, баг-репорты
```

## Сводка итераций

| # | Backend | Клиент | Результат |
|---|---------|--------|-----------|
| 0 | Каркас, справочники, `GET /config` | KMP/CMP shell, mock API, tabs | Локальный запуск обеих частей |
| 1 | Auth, JWT, notification preferences | SCR-001, SCR-002, LOGIC-001–002 | Регистрация и сессия |
| 2 | Расписание, допуски, availability | SCR-003, SCR-004, SCR-013 | Просмотр слотов 7 дней |
| 3 | `POST /bookings`, pricing, concurrency | SCR-005, SCR-015, LOGIC-005–007 | Запись на тренировку |
| 4 | Список записей, отмена, политика | SCR-006–008, LOGIC-008 | Мои записи и отмена |
| 5 | `PATCH …/rental` | LOGIC-010 на SCR-007 | Изменение проката |
| 6 | Gym cancel, alternatives | SCR-009, LOGIC-009 | Альтернативный слот |
| 7 | Push token, worker jobs | SCR-010, SCR-011, LOGIC-011 | Профиль и настройки |
| 8 | Санкции, contract/load tests, CI | SCR-014, LOGIC-012–013, hardening | Push и release-ready |
| PM | `POST /ratings` | SCR-012, LOGIC-014 | Оценка инструктора |

## Точка входа для разработчиков

| Роль | С чего начать |
|------|----------------|
| Backend | [`backend/README.md`](../backend/README.md) → `make setup` → `make run` |
| Mobile | [`client/README.md`](../client/README.md) → `./gradlew :composeApp:assembleDebug` |
| QA | [`03-test/`](../03-test/) — план покрытия, JSON-реестры тестов, баг-репорты автотестов |

## Связанные документы

- [Анализ и требования](../01-analysis/README.md)
- [OpenAPI-контракт](../01-analysis/api/openapi.yaml)
- [Mobile app spec](../01-analysis/5-mobile-app-spec/README.md)
- [План покрытия тестами](../03-test/test-coverage-plan.md)
