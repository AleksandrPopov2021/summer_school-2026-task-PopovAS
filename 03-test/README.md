# Тестирование — Скалодром «Вертикаль»

Каталог артефактов этапа QA для мобильного клиента (Compose Multiplatform) и backend API (Go).

**Дата этапа:** 2026-07-05  
**Область:** backend API + client shared (unit/integration) + Android UI smoke

---

## Выполненные работы

### 1. Планирование и инвентаризация покрытия

- Составлен [высокоуровневый план покрытия](test-coverage-plan.md) по пирамиде тестирования, доменам приложения, FR/UC, OpenAPI operationId, клиентским логикам (LOGIC-001 … LOGIC-014) и NFR.
- Для каждого раздела плана подготовлены машиночитаемые JSON-списки тест-кейсов с привязкой к требованиям, статусу автоматизации и ссылкам на реализованные тесты в коде.
- Оглавление разделов — в [tests-index.json](tests-index.json).

### 2. Прогон автотестов

Выполнен полный локальный прогон на Windows 10 с PostgreSQL (Docker) и Android-эмулятором. Подробный отчёт — [autotest-bug-report.md](autotest-bug-report.md).

| Компонент | Результат | Пройдено | Провалено | Пропущено |
|-----------|-----------|----------|-----------|-----------|
| Backend (Go) | ✅ | 72 | 0 | 0 |
| Client shared (Kotlin) | ✅ | 81 | 0 | 0 |
| Android UI / E2E | ⚠️ Smoke OK | 1 | 0 | 1 |

**Backend:** unit-тесты domain/application (телефон, JWT, политика отмены, pricing, loyalty, rating, push routing), HTTP integration с PostgreSQL (клиенты, слоты, бронирование, отмена, gym cancel, alternatives, push token, ratings, worker), contract smoke по MVP paths, нагрузочный тест 50 concurrent bookings за последнее место.

**Client shared:** unit-тесты use cases и domain logic (сессия, регистрация, расписание, доступность слота, бронирование, отмена, прокат, push routing, rating), integration-тесты data layer с mock engine (auth, conflict 409, update rental).

**Android UI:** smoke-тест `authenticated_user_sees_schedule_slots` (расписание, mock API); полный E2E `register_book_and_cancel` помечен `@Ignore` из-за ограничений Compose Multiplatform UI Test.

### 3. Выявленные дефекты и исправления

| ID | Описание | Статус |
|----|----------|--------|
| [BUG-001](BUG-001-clearance-integration-flaky.md) | Flaky integration-тест: отказ без допуска на «трассы» выбирал слот вне окна записи → 422 вместо 403 | ✅ Исправлено |
| [BUG-002](BUG-002-push-routing-deep-link-test.md) | Unit-тест deep link: неверное ожидание `cancelledSlotId` в `PushRoutingTest` | ✅ Исправлено |
| [BUG-003](BUG-003-android-ui-espresso-inputmanager.md) | Падение `MvpFlowTest` на Espresso/InputManager; нестабильность CMP UI Test | ⚠️ Частично: smoke проходит, полный E2E — backlog |

Дополнительно устранены проблемы инфраструктуры: восстановлен `gradle-wrapper.jar`, исправлена компиляция client unit-тестов (`MapSettings`, дубликаты fake-репозиториев), обновлены AndroidX Test/Espresso, настроен mock API для UI smoke.

### 4. Текущее состояние покрытия (кратко)

- **Критичные сценарии UC-002 (запись), UC-004 (отмена gym), UC-006 (регистрация), UC-007 (лояльность)** — покрыты автотестами на backend и/или client unit.
- **Backend CI** — настроен (`.github/workflows/backend-ci.yml`: migrate → seed → `go test`, golangci-lint).
- **Client CI** — unit и Android UI тесты пока запускаются только локально.
- **Ручное exploratory-тестирование** — чек-листы для smoke, offline, push, accessibility и regression описаны в разделе 10 плана покрытия.
- **Backlog автоматизации** — 9 приоритетных задач (Client CI, PATCH notification-preferences, staging E2E, UI empty state, worker reminders и др.) — см. [tests-09-backlog.json](tests-09-backlog.json).

---

## Документы

| Документ | Описание |
|----------|----------|
| [test-coverage-plan.md](test-coverage-plan.md) | Высокоуровневый план покрытия (матрица по доменам, уровням и статусу) |
| [autotest-bug-report.md](autotest-bug-report.md) | Отчёт о прогоне автотестов, найденных дефектах и инфраструктурных проблемах |
| [tests-index.json](tests-index.json) | Оглавление JSON-списков тестов по разделам плана |

### JSON-списки тестов (по разделам плана)

| Файл | Раздел |
|------|--------|
| [tests-01-pyramid.json](tests-01-pyramid.json) | 1. Пирамида тестирования |
| [tests-02-domains.json](tests-02-domains.json) | 2. Домены приложения |
| [tests-03-fr.json](tests-03-fr.json) | 3. FR-001 … FR-032 |
| [tests-04-uc.json](tests-04-uc.json) | 4. UC-001 … UC-012 |
| [tests-05-api.json](tests-05-api.json) | 5. OpenAPI operationId |
| [tests-06-logic.json](tests-06-logic.json) | 6. LOGIC-001 … LOGIC-014 |
| [tests-07-nfr.json](tests-07-nfr.json) | 7. NFR-001 … NFR-009 |
| [tests-08-ci.json](tests-08-ci.json) | 8. CI/CD |
| [tests-09-backlog.json](tests-09-backlog.json) | 9. Backlog автоматизации |
| [tests-10-manual.json](tests-10-manual.json) | 10. Ручное тестирование |

### Отчёты о дефектах

| Файл | Описание |
|------|----------|
| [BUG-001-clearance-integration-flaky.md](BUG-001-clearance-integration-flaky.md) | Flaky integration-тест допуска на «трассы» |
| [BUG-002-push-routing-deep-link-test.md](BUG-002-push-routing-deep-link-test.md) | Ошибка ожиданий в unit-тесте deep link |
| [BUG-003-android-ui-espresso-inputmanager.md](BUG-003-android-ui-espresso-inputmanager.md) | Android UI smoke / Espresso / CMP limitations |
| [logcat-ui-test.txt](logcat-ui-test.txt) | Лог Android UI-теста (диагностика) |

---

## Связанные материалы

- [Функциональные требования](../01-analysis/2-requirements/functional-requirements.md) (FR-001 … FR-032)
- [Use Cases](../01-analysis/2-requirements/use-cases.md) (UC-001 … UC-012)
- [Спецификация мобильного приложения](../01-analysis/5-mobile-app-spec/README.md)
- [OpenAPI](../01-analysis/api/openapi.yaml)
- [План реализации backend](../02-development/backend-implementation-plan.md)
- [План реализации клиента](../02-development/client-implementation-plan.md)

---

## Запуск автотестов

```bash
# Backend (PostgreSQL должен быть доступен)
cd backend && make migrate seed && go test ./... -count=1

# Client — unit (shared/commonTest)
cd client && ./gradlew :shared:cleanAllTests :shared:allTests

# Client — Android UI smoke (эмулятор/устройство, API 26+)
cd client && ./gradlew :composeApp:connectedDebugAndroidTest
```
