# 03. Запись

Домен мобильного приложения «Вертикаль»: оформление записи на тренировку и подтверждение согласия на риск.

**Релиз:** 1.0.0  
**Статус раздела:** Актуален

---

## Экраны

| ID | Название | Файл | Приоритет | Статус |
|----|----------|------|-----------|--------|
| SCR-005 | Экран оформления записи | [SCR-005_Booking-Screen.md](SCR-005_Booking-Screen.md) | Critical | Актуален |
| SCR-015 | Экран согласия на риск | [SCR-015_Consent-Screen.md](SCR-015_Consent-Screen.md) | Critical | Актуален *(создан другим агентом)* |

---

## Применяемые логики

| Логика | Экраны | Описание |
|--------|--------|----------|
| [LOGIC-005](../09_Logics/LOGIC-005_Создание-записи-на-тренировку.md) | SCR-005 | Создание записи через API |
| [LOGIC-006](../09_Logics/LOGIC-006_Подтверждение-согласия-на-риск.md) | SCR-005 → SCR-015 | Проверка и сохранение согласия на риск |
| [LOGIC-007](../09_Logics/LOGIC-007_Выбор-проката-и-расчёт-стоимости.md) | SCR-005 | Выбор снаряжения и расчёт итоговой стоимости |

---

## Связанные FR

FR-010, FR-011, FR-012, FR-013, FR-014

---

## API

Спецификация: [openapi.yaml](../../api/openapi.yaml)

| operationId | Метод | Путь |
|-------------|-------|------|
| `listRentalEquipmentTypes` | GET | `/rental-equipment-types` |
| `getSlotRentalAvailability` | GET | `/slots/{slotId}/rental-availability` |
| `createBooking` | POST | `/bookings` |
| `updateCurrentClient` | PATCH | `/clients/me` |
