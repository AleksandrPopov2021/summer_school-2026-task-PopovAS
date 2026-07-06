package postgres

import (
	"context"
	"fmt"
	"time"

	"github.com/google/uuid"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/vertical-climbing/backend/internal/adapters/postgres/db"
	"github.com/vertical-climbing/backend/internal/domain"
	portworker "github.com/vertical-climbing/backend/internal/ports/worker"
)

type WorkerRepository struct {
	pool *pgxpool.Pool
}

func NewWorkerRepository(pool *pgxpool.Pool) *WorkerRepository {
	return &WorkerRepository{pool: pool}
}

func (r *WorkerRepository) ListBookingsForDayBeforeReminder(ctx context.Context, now time.Time) ([]portworker.ReminderTarget, error) {
	queries := db.New(r.pool)
	rows, err := queries.ListBookingsForDayBeforeReminder(ctx, now)
	if err != nil {
		return nil, fmt.Errorf("list day-before reminders: %w", err)
	}

	return mapReminderRows(rows), nil
}

func (r *WorkerRepository) ListBookingsForHoursBeforeReminder(
	ctx context.Context,
	now time.Time,
	hoursBefore int32,
) ([]portworker.ReminderTarget, error) {
	queries := db.New(r.pool)
	rows, err := queries.ListBookingsForHoursBeforeReminder(ctx, db.ListBookingsForHoursBeforeReminderParams{
		Now:          now,
		HoursBefore:  hoursBefore,
	})
	if err != nil {
		return nil, fmt.Errorf("list hours-before reminders: %w", err)
	}

	return mapReminderRows(rows), nil
}

func (r *WorkerRepository) ListBookingsToComplete(ctx context.Context, now time.Time) ([]portworker.CompleteTarget, error) {
	queries := db.New(r.pool)
	rows, err := queries.ListBookingsToComplete(ctx, now)
	if err != nil {
		return nil, fmt.Errorf("list bookings to complete: %w", err)
	}

	targets := make([]portworker.CompleteTarget, 0, len(rows))
	for _, row := range rows {
		targets = append(targets, portworker.CompleteTarget{
			BookingID: row.BookingID.String(),
			ClientID:  row.ClientID.String(),
			SlotID:    row.SlotID.String(),
		})
	}
	return targets, nil
}

func (r *WorkerRepository) CompleteBooking(ctx context.Context, bookingID, clientID string, visitsForLoyalty int32) error {
	parsedBookingID, err := uuid.Parse(bookingID)
	if err != nil {
		return fmt.Errorf("parse booking id: %w", err)
	}
	parsedClientID, err := uuid.Parse(clientID)
	if err != nil {
		return fmt.Errorf("parse client id: %w", err)
	}

	tx, err := r.pool.Begin(ctx)
	if err != nil {
		return fmt.Errorf("begin transaction: %w", err)
	}
	defer tx.Rollback(ctx)

	queries := db.New(tx)

	rowsAffected, err := queries.CompleteBooking(ctx, parsedBookingID)
	if err != nil {
		return fmt.Errorf("complete booking: %w", err)
	}
	if rowsAffected == 0 {
		return nil
	}

	clientRow, err := queries.IncrementCompletedVisits(ctx, parsedClientID)
	if err != nil {
		return fmt.Errorf("increment completed visits: %w", err)
	}

	loyalty := domain.BuildLoyaltyStatus(clientRow.CompletedVisitsCount, visitsForLoyalty)
	var discountValue *string
	if loyalty.LoyaltyDiscount != nil {
		value := loyalty.LoyaltyDiscount.String()
		discountValue = &value
	}

	if _, err := queries.UpdateClientLoyalty(ctx, parsedClientID, loyalty.IsLoyalClient, discountValue); err != nil {
		return fmt.Errorf("update client loyalty: %w", err)
	}

	if err := tx.Commit(ctx); err != nil {
		return fmt.Errorf("commit transaction: %w", err)
	}
	return nil
}

func (r *WorkerRepository) RecordSentPush(ctx context.Context, bookingID, notificationType string) error {
	parsedBookingID, err := uuid.Parse(bookingID)
	if err != nil {
		return fmt.Errorf("parse booking id: %w", err)
	}

	queries := db.New(r.pool)
	if err := queries.RecordSentPushNotification(ctx, parsedBookingID, notificationType); err != nil {
		return fmt.Errorf("record sent push: %w", err)
	}
	return nil
}

func mapReminderRows(rows []db.ReminderTargetRow) []portworker.ReminderTarget {
	targets := make([]portworker.ReminderTarget, 0, len(rows))
	for _, row := range rows {
		targets = append(targets, portworker.ReminderTarget{
			BookingID: row.BookingID.String(),
			ClientID:  row.ClientID.String(),
			SlotID:    row.SlotID.String(),
		})
	}
	return targets
}
