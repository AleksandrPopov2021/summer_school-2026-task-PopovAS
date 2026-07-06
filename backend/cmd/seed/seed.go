package main

import (
	"context"
	"fmt"

	"github.com/jackc/pgx/v5/pgxpool"
)

func seed(ctx context.Context, pool *pgxpool.Pool) error {
	tx, err := pool.Begin(ctx)
	if err != nil {
		return fmt.Errorf("begin transaction: %w", err)
	}
	defer tx.Rollback(ctx)

	if _, err := tx.Exec(ctx, `DELETE FROM system_config`); err != nil {
		return fmt.Errorf("clear system_config: %w", err)
	}
	if _, err := tx.Exec(ctx, `DELETE FROM payments`); err != nil {
		return fmt.Errorf("clear payments: %w", err)
	}
	if _, err := tx.Exec(ctx, `DELETE FROM booking_rental_lines`); err != nil {
		return fmt.Errorf("clear booking_rental_lines: %w", err)
	}
	if _, err := tx.Exec(ctx, `DELETE FROM bookings`); err != nil {
		return fmt.Errorf("clear bookings: %w", err)
	}
	if _, err := tx.Exec(ctx, `DELETE FROM slot_rental_availability`); err != nil {
		return fmt.Errorf("clear slot_rental_availability: %w", err)
	}
	if _, err := tx.Exec(ctx, `DELETE FROM training_slots`); err != nil {
		return fmt.Errorf("clear training_slots: %w", err)
	}
	if _, err := tx.Exec(ctx, `DELETE FROM instructor_clearances`); err != nil {
		return fmt.Errorf("clear instructor_clearances: %w", err)
	}
	if _, err := tx.Exec(ctx, `DELETE FROM cancellation_reasons`); err != nil {
		return fmt.Errorf("clear cancellation_reasons: %w", err)
	}
	if _, err := tx.Exec(ctx, `DELETE FROM rental_equipment_types`); err != nil {
		return fmt.Errorf("clear rental_equipment_types: %w", err)
	}
	if _, err := tx.Exec(ctx, `DELETE FROM instructors`); err != nil {
		return fmt.Errorf("clear instructors: %w", err)
	}
	if _, err := tx.Exec(ctx, `DELETE FROM training_zones`); err != nil {
		return fmt.Errorf("clear training_zones: %w", err)
	}
	if _, err := tx.Exec(ctx, `DELETE FROM gym_venues`); err != nil {
		return fmt.Errorf("clear gym_venues: %w", err)
	}

	if _, err := tx.Exec(ctx, `
		INSERT INTO gym_venues (id, name, address)
		VALUES (
			'a0000000-0000-4000-8000-000000000001',
			'Вертикаль',
			'г. Москва, ул. Скалолазная, д. 1'
		)`); err != nil {
		return fmt.Errorf("insert gym venue: %w", err)
	}

	zones := []struct {
		id, name, formatType, difficulty string
		maxGroupSize                     int
	}{
		{
			id:           "b0000000-0000-4000-8000-000000000001",
			name:         "Болдеринг с инструктажем",
			formatType:   "bouldering_instruction",
			difficulty:   "beginner",
			maxGroupSize: 8,
		},
		{
			id:           "b0000000-0000-4000-8000-000000000002",
			name:         "Трассы с верёвкой",
			formatType:   "rope_routes",
			difficulty:   "experienced",
			maxGroupSize: 16,
		},
	}
	for _, zone := range zones {
		if _, err := tx.Exec(ctx, `
			INSERT INTO training_zones (id, name, format_type, difficulty, max_group_size)
			VALUES ($1, $2, $3, $4, $5)`,
			zone.id, zone.name, zone.formatType, zone.difficulty, zone.maxGroupSize,
		); err != nil {
			return fmt.Errorf("insert training zone %s: %w", zone.name, err)
		}
	}

	instructors := []struct {
		id, fullName string
	}{
		{"c0000000-0000-4000-8000-000000000001", "Петров Алексей Сергеевич"},
		{"c0000000-0000-4000-8000-000000000002", "Сидорова Мария Ивановна"},
		{"c0000000-0000-4000-8000-000000000003", "Козлов Дмитрий Петрович"},
		{"c0000000-0000-4000-8000-000000000004", "Новикова Елена Александровна"},
	}
	for _, instructor := range instructors {
		if _, err := tx.Exec(ctx, `
			INSERT INTO instructors (id, full_name)
			VALUES ($1, $2)`,
			instructor.id, instructor.fullName,
		); err != nil {
			return fmt.Errorf("insert instructor %s: %w", instructor.fullName, err)
		}
	}

	equipment := []struct {
		id, code, name string
		price          string
	}{
		{"d0000000-0000-4000-8000-000000000001", "shoes", "Скальные туфли", "350.00"},
		{"d0000000-0000-4000-8000-000000000002", "harness", "Страховочная система", "250.00"},
		{"d0000000-0000-4000-8000-000000000003", "helmet", "Каска", "150.00"},
		{"d0000000-0000-4000-8000-000000000004", "chalk", "Магнезия", "80.00"},
	}
	for _, item := range equipment {
		if _, err := tx.Exec(ctx, `
			INSERT INTO rental_equipment_types (id, code, name, default_price)
			VALUES ($1, $2, $3, $4)`,
			item.id, item.code, item.name, item.price,
		); err != nil {
			return fmt.Errorf("insert rental equipment %s: %w", item.code, err)
		}
	}

	reasons := []struct {
		id, code, title, apology string
	}{
		{
			id:      "e0000000-0000-4000-8000-000000000001",
			code:    "instructor_unavailable",
			title:   "Инструктор недоступен",
			apology: "Приносим извинения за неудобства. Инструктор не может провести тренировку.",
		},
		{
			id:      "e0000000-0000-4000-8000-000000000002",
			code:    "equipment_maintenance",
			title:   "Техническое обслуживание",
			apology: "Приносим извинения — зона временно закрыта на обслуживание.",
		},
		{
			id:      "e0000000-0000-4000-8000-000000000003",
			code:    "low_attendance",
			title:   "Недостаточная запись",
			apology: "К сожалению, группа не набралась. Мы свяжемся с вами для переноса.",
		},
		{
			id:      "e0000000-0000-4000-8000-000000000004",
			code:    "venue_closure",
			title:   "Закрытие скалодрома",
			apology: "Скалодром временно закрыт. Приносим извинения за доставленные неудобства.",
		},
	}
	for _, reason := range reasons {
		if _, err := tx.Exec(ctx, `
			INSERT INTO cancellation_reasons (id, code, title, apology_text)
			VALUES ($1, $2, $3, $4)`,
			reason.id, reason.code, reason.title, reason.apology,
		); err != nil {
			return fmt.Errorf("insert cancellation reason %s: %w", reason.code, err)
		}
	}

	if _, err := tx.Exec(ctx, `
		INSERT INTO system_config (
			id,
			reminder_hours_before,
			visits_for_loyalty,
			violations_for_sanctions,
			booking_cutoff_minutes,
			cancellation_forbidden_minutes
		) VALUES (
			'f0000000-0000-4000-8000-000000000001',
			3,
			10,
			3,
			30,
			60
		)`); err != nil {
		return fmt.Errorf("insert system_config: %w", err)
	}

	if err := seedSlotsAndTestClient(ctx, tx); err != nil {
		return err
	}

	return tx.Commit(ctx)
}
