package main

import (
	"context"
	"fmt"
	"time"

	"github.com/jackc/pgx/v5"
)

const (
	testClientID     = "10000000-0000-4000-8000-000000000001"
	testClientPhone  = "+79009999999"
	venueID          = "a0000000-0000-4000-8000-000000000001"
	zoneBoulderingID = "b0000000-0000-4000-8000-000000000001"
	zoneRopeID       = "b0000000-0000-4000-8000-000000000002"
	instructor1ID    = "c0000000-0000-4000-8000-000000000001"
	instructor2ID    = "c0000000-0000-4000-8000-000000000002"
	instructor3ID    = "c0000000-0000-4000-8000-000000000003"
	equipmentShoesID = "d0000000-0000-4000-8000-000000000001"
	equipmentHarness = "d0000000-0000-4000-8000-000000000002"
	equipmentHelmet  = "d0000000-0000-4000-8000-000000000003"
	equipmentChalk   = "d0000000-0000-4000-8000-000000000004"
	venueAddress     = "г. Москва, ул. Скалолазная, д. 1"
)

func seedSlotsAndTestClient(ctx context.Context, tx pgx.Tx) error {
	if err := seedTestClient(ctx, tx); err != nil {
		return err
	}
	return seedTrainingSlots(ctx, tx)
}

func seedTestClient(ctx context.Context, tx pgx.Tx) error {
	if _, err := tx.Exec(ctx, `DELETE FROM instructor_clearances`); err != nil {
		return fmt.Errorf("clear instructor_clearances: %w", err)
	}
	if _, err := tx.Exec(ctx, `DELETE FROM notification_preferences WHERE client_id = $1`, testClientID); err != nil {
		return fmt.Errorf("clear test notification preferences: %w", err)
	}
	if _, err := tx.Exec(ctx, `DELETE FROM clients WHERE id = $1`, testClientID); err != nil {
		return fmt.Errorf("clear test client: %w", err)
	}

	if _, err := tx.Exec(ctx, `
		INSERT INTO clients (id, full_name, phone, birth_date, risk_consent_accepted)
		VALUES ($1, 'Тестовый Клиент', $2, '1990-01-01', TRUE)`,
		testClientID, testClientPhone,
	); err != nil {
		return fmt.Errorf("insert test client: %w", err)
	}

	if _, err := tx.Exec(ctx, `
		INSERT INTO notification_preferences (client_id)
		VALUES ($1)`,
		testClientID,
	); err != nil {
		return fmt.Errorf("insert test notification preferences: %w", err)
	}

	if _, err := tx.Exec(ctx, `
		INSERT INTO instructor_clearances (id, client_id, instructor_id, is_granted, granted_at)
		VALUES (
			'11000000-0000-4000-8000-000000000001',
			$1,
			$2,
			TRUE,
			NOW()
		)`,
		testClientID, instructor1ID,
	); err != nil {
		return fmt.Errorf("insert test clearance: %w", err)
	}

	return nil
}

func seedTrainingSlots(ctx context.Context, tx pgx.Tx) error {
	if _, err := tx.Exec(ctx, `DELETE FROM slot_rental_availability`); err != nil {
		return fmt.Errorf("clear slot_rental_availability: %w", err)
	}
	if _, err := tx.Exec(ctx, `DELETE FROM training_slots`); err != nil {
		return fmt.Errorf("clear training_slots: %w", err)
	}

	now := time.Now()
	startDay := time.Date(now.Year(), now.Month(), now.Day(), 0, 0, 0, 0, now.Location())

	templates := []struct {
		hour, minute int
		zoneID       string
		instructorID string
		capacity     int
		freeSpots    int
		trainingPrice,
		rentalTariff string
		slotStatus string
	}{
		{10, 0, zoneBoulderingID, instructor1ID, 8, 5, "1200.00", "400.00", "active"},
		{14, 0, zoneBoulderingID, instructor2ID, 8, 0, "1200.00", "400.00", "active"},
		{18, 0, zoneRopeID, instructor3ID, 16, 7, "1500.00", "500.00", "active"},
	}

	slotIndex := 0
	for day := 0; day < 14; day++ {
		for _, template := range templates {
			startsAt := startDay.AddDate(0, 0, day).Add(
				time.Duration(template.hour)*time.Hour + time.Duration(template.minute)*time.Minute,
			)
			if !startsAt.After(now) {
				continue
			}

			slotIndex++
			status := template.slotStatus
			if day == 3 && template.hour == 14 {
				status = "cancelled_by_gym"
			}
			if day == 7 && template.hour == 18 {
				status = "cancelled_by_gym"
			}

			slotID := fmt.Sprintf("e0000000-0000-4000-8000-%012d", slotIndex)
			if _, err := tx.Exec(ctx, `
				INSERT INTO training_slots (
					id, starts_at, duration_minutes, capacity, free_spots,
					training_price, rental_tariff, slot_status, address,
					zone_id, instructor_id, venue_id
				) VALUES (
					$1, $2, 90, $3, $4, $5, $6, $7, $8, $9, $10, $11
				)`,
				slotID,
				startsAt,
				template.capacity,
				template.freeSpots,
				template.trainingPrice,
				template.rentalTariff,
				status,
				venueAddress,
				template.zoneID,
				template.instructorID,
				venueID,
			); err != nil {
				return fmt.Errorf("insert slot %s: %w", slotID, err)
			}

			rentalStock := []struct {
				equipmentID string
				quantity    int
			}{
				{equipmentShoesID, 10},
				{equipmentHarness, 8},
				{equipmentHelmet, 6},
				{equipmentChalk, 12},
			}
			for i, stock := range rentalStock {
				if _, err := tx.Exec(ctx, `
					INSERT INTO slot_rental_availability (id, slot_id, equipment_type_id, available_quantity)
					VALUES ($1, $2, $3, $4)`,
					fmt.Sprintf("f0000000-0000-4000-8000-%012d", slotIndex*10+i+1),
					slotID,
					stock.equipmentID,
					stock.quantity,
				); err != nil {
					return fmt.Errorf("insert rental availability: %w", err)
				}
			}
		}
	}

	return nil
}
