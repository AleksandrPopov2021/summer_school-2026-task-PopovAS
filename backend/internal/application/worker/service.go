package worker

import (
	"context"
	"fmt"
	"log/slog"
	"time"

	apppush "github.com/vertical-climbing/backend/internal/application/push"
	portref "github.com/vertical-climbing/backend/internal/ports/reference"
	portworker "github.com/vertical-climbing/backend/internal/ports/worker"
)

type Service struct {
	repo      portworker.Repository
	reference portref.Repository
	push      *apppush.Service
	now       func() time.Time
}

func NewService(
	repo portworker.Repository,
	reference portref.Repository,
	push *apppush.Service,
) *Service {
	return &Service{
		repo:      repo,
		reference: reference,
		push:      push,
		now:       time.Now,
	}
}

func (s *Service) RunAll(ctx context.Context) error {
	if err := s.SendReminders(ctx); err != nil {
		return fmt.Errorf("send reminders: %w", err)
	}
	if err := s.CompleteBookings(ctx); err != nil {
		return fmt.Errorf("complete bookings: %w", err)
	}
	return nil
}

func (s *Service) SendReminders(ctx context.Context) error {
	now := s.now()

	dayBefore, err := s.repo.ListBookingsForDayBeforeReminder(ctx, now)
	if err != nil {
		return err
	}
	for _, target := range dayBefore {
		if s.push == nil {
			break
		}
		if err := s.push.SendDayBeforeReminder(ctx, target.ClientID, target.BookingID, target.SlotID); err != nil {
			slog.Warn("day-before reminder failed", "error", err, "booking_id", target.BookingID)
		}
	}

	config, err := s.reference.GetSystemConfig(ctx)
	if err != nil {
		return err
	}

	hoursBefore, err := s.repo.ListBookingsForHoursBeforeReminder(ctx, now, config.ReminderHoursBefore)
	if err != nil {
		return err
	}
	for _, target := range hoursBefore {
		if s.push == nil {
			break
		}
		if err := s.push.SendHoursBeforeReminder(ctx, target.ClientID, target.BookingID, target.SlotID); err != nil {
			slog.Warn("hours-before reminder failed", "error", err, "booking_id", target.BookingID)
		}
	}

	return nil
}

func (s *Service) CompleteBookings(ctx context.Context) error {
	config, err := s.reference.GetSystemConfig(ctx)
	if err != nil {
		return err
	}

	targets, err := s.repo.ListBookingsToComplete(ctx, s.now())
	if err != nil {
		return err
	}

	for _, target := range targets {
		if err := s.repo.CompleteBooking(ctx, target.BookingID, target.ClientID, config.VisitsForLoyalty); err != nil {
			slog.Warn("complete booking failed", "error", err, "booking_id", target.BookingID)
			continue
		}
		if s.push == nil {
			continue
		}
		if err := s.push.SendRatingInvitation(ctx, target.ClientID, target.BookingID, target.SlotID); err != nil {
			slog.Warn("rating invitation push failed", "error", err, "booking_id", target.BookingID)
		}
	}

	return nil
}
