# 06. Уведомления — индекс экранов

**Домен:** 06. Уведомления  
**Приложение:** Скалодром «Вертикаль»  
**Релиз:** 1.0.0

---

## Экраны домена

| ID | Название | Файл ТЗ | Приоритет | Зона авторизации | Статус |
|----|----------|---------|-----------|------------------|--------|
| SCR-014 | Push Notification View | [SCR-014_Push-Notification-View.md](SCR-014_Push-Notification-View.md) | High | АЗ | Актуален |

> **Примечание:** SCR-014 не является самостоятельным пунктом нижней навигации. Экран открывается при тапе на push-уведомление или переходе по deep link из payload уведомления.

---

## Связанные логики

| Логика | Экраны | Описание |
|--------|--------|----------|
| [LOGIC-013](../09_Logics/LOGIC-013_Deep-link-push-routing.md) | SCR-014 | Маршрутизация по типу push/deep link на целевые экраны |

---

## Навигация домена

```mermaid
flowchart TD
    Push[Push / Deep Link] --> SCR014[SCR-014 Push View]
    SCR014 -->|type=booking_confirmed| SCR006[SCR-006 Мои записи]
    SCR014 -->|type=reminder| SCR007[SCR-007 Детали записи]
    SCR014 -->|type=gym_cancellation| SCR009[SCR-009 Альтернатива]
    SCR014 -->|type=rating_invitation Post-MVP| SCR012[SCR-012 Оценка]
```

---

## Связанные требования

- [FR-015, FR-020, FR-024, FR-025, FR-031](../../2-requirements/functional-requirements.md) — push-уведомления и приглашение к оценке
- [DB-014](../../3-design-brief/design-briefs.md) — постановка на дизайн
