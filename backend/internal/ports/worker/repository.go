package worker

import (
	"context"
	"time"
)

type ReminderTarget struct {
	BookingID string
	ClientID  string
	SlotID    string
}

type CompleteTarget struct {
	BookingID string
	ClientID  string
	SlotID    string
}

type Repository interface {
	ListBookingsForDayBeforeReminder(ctx context.Context, now time.Time) ([]ReminderTarget, error)
	ListBookingsForHoursBeforeReminder(ctx context.Context, now time.Time, hoursBefore int32) ([]ReminderTarget, error)
	ListBookingsToComplete(ctx context.Context, now time.Time) ([]CompleteTarget, error)
	CompleteBooking(ctx context.Context, bookingID, clientID string, visitsForLoyalty int32) error
	RecordSentPush(ctx context.Context, bookingID, notificationType string) error
}
