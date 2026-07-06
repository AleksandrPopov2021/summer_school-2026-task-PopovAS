# Клиент «Вертикаль» (Compose Multiplatform)

Мобильное приложение скалодрома «Вертикаль» на **Kotlin Multiplatform + Compose Multiplatform**
(Android + iOS). MVP-клиент (итерации 0–9) реализован по
[плану реализации клиента](../02-development/client-implementation-plan.md).

Контракт API: [`01-analysis/api/openapi.yaml`](../01-analysis/api/openapi.yaml).

## Стек

| Слой | Технологии |
|------|-----------|
| UI | Compose Multiplatform, Material 3 |
| Навигация | Decompose (RootComponent / child stack) |
| DI | Koin |
| Сеть | Ktor client, kotlinx.serialization |
| Хранилище | multiplatform-settings (secure + cache), expect/actual |
| Push | FCM (Android), APNs (iOS) |
| Архитектура | Clean Architecture + MVI |

## Структура модулей

```
client/
├── shared/                     # KMP-библиотека (бизнес-логика)
│   └── src/commonMain/kotlin/ru/vertical/climbing/
│       ├── domain/             # model, repository, usecase, util
│       ├── data/               # remote, local, mapper, repository impl
│       ├── presentation/       # Async, offline fallback
│       └── di/                 # Koin-модули
├── composeApp/                 # KMP-приложение (UI + точки входа)
├── iosApp/                     # Xcode-обёртка
└── gradle/libs.versions.toml
```

## MVP-функциональность (14 экранов, LOGIC-001…013)

- Авторизация и регистрация (SCR-001, SCR-002)
- Расписание и детали слота (SCR-003, SCR-004, SCR-013)
- Запись, согласие на риск, прокат (SCR-005, SCR-015)
- Мои записи, отмена, изменение проката (SCR-006–008)
- Альтернативный слот при отмене скалодромом (SCR-009)
- Профиль, лояльность, настройки push (SCR-010, SCR-011)
- Push-маршрутизация (SCR-014)

## Требования к окружению

- JDK 17
- Android SDK (compileSdk 35), устройство/эмулятор API 26+
- Для iOS: macOS + Xcode 15+

## Сборка и запуск

### Gradle wrapper (один раз)

```bash
cd client
gradle wrapper --gradle-version 8.10.2
```

### Android (debug, mock API по умолчанию)

```bash
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:installDebug
```

Release-сборка с R8/ProGuard:

```bash
./gradlew :composeApp:assembleRelease
```

### iOS

Откройте `iosApp/iosApp.xcodeproj` в Xcode и запустите таргет `iosApp`.

### Тесты

```bash
# Unit-тесты shared (все use cases MVP)
./gradlew :shared:allTests

# Android UI smoke (эмулятор/устройство): register → book → cancel
./gradlew :composeApp:connectedDebugAndroidTest
```

## Конфигурация окружения

| Режим | Как включить |
|-------|--------------|
| **Mock API** (по умолчанию в debug) | `ApiEnvStore` / debug-панель в профиле |
| **Staging backend** | Debug → Профиль → «Debug: окружение API» → STAGING, выключить Mock → перезапуск |
| **Production** | Release-сборка автоматически: `PROD`, `useMock=false` |

Переменные окружения не требуются — base URL задаётся в `ApiEnvironment` (`shared/.../data/remote/ApiConfig.kt`).

Debug-only переключатель: **Профиль → Debug: окружение API** (dev/staging/prod + mock toggle).

## Staging / internal distribution

1. Соберите release или debug APK: `./gradlew :composeApp:assembleRelease`
2. APK: `composeApp/build/outputs/apk/release/`
3. iOS: Archive в Xcode → TestFlight (bundle id и signing настраиваются в Xcode)

Backend staging: `https://staging-api.vertical-climbing.ru/v1`

## Связанные документы

- [План реализации клиента](../02-development/client-implementation-plan.md)
- [Mobile app spec](../01-analysis/5-mobile-app-spec/README.md)
