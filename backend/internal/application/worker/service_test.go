package worker_test

import (
	"context"
	"testing"
	"time"

	"github.com/vertical-climbing/backend/internal/domain"
	portref "github.com/vertical-climbing/backend/internal/ports/reference"
	portworker "github.com/vertical-climbing/backend/internal/ports/worker"
	appworker "github.com/vertical-climbing/backend/internal/application/worker"
)

type stubWorkerRepo struct {
	completeCalls []completeCall
}

type completeCall struct {
	bookingID        string
	clientID         string
	visitsForLoyalty int32
}

func (s *stubWorkerRepo) ListBookingsForDayBeforeReminder(_ context.Context, _ time.Time) ([]portworker.ReminderTarget, error) {
	return nil, nil
}

func (s *stubWorkerRepo) ListBookingsForHoursBeforeReminder(_ context.Context, _ time.Time, _ int32) ([]portworker.ReminderTarget, error) {
	return nil, nil
}

func (s *stubWorkerRepo) ListBookingsToComplete(_ context.Context, _ time.Time) ([]portworker.CompleteTarget, error) {
	return []portworker.CompleteTarget{{BookingID: "booking-1", ClientID: "client-1"}}, nil
}

func (s *stubWorkerRepo) CompleteBooking(_ context.Context, bookingID, clientID string, visitsForLoyalty int32) error {
	s.completeCalls = append(s.completeCalls, completeCall{
		bookingID:        bookingID,
		clientID:         clientID,
		visitsForLoyalty: visitsForLoyalty,
	})
	return nil
}

func (s *stubWorkerRepo) RecordSentPush(_ context.Context, _, _ string) error {
	return nil
}

type stubReferenceRepo struct {
	config portref.SystemConfig
}

func (s *stubReferenceRepo) GetSystemConfig(_ context.Context) (portref.SystemConfig, error) {
	return s.config, nil
}

func (s *stubReferenceRepo) ListRentalEquipmentTypes(_ context.Context) ([]portref.RentalEquipmentType, error) {
	return nil, nil
}

func TestCompleteBookings_UsesLoyaltyThreshold(t *testing.T) {
	repo := &stubWorkerRepo{}
	service := appworker.NewService(repo, &stubReferenceRepo{
		config: portref.SystemConfig{VisitsForLoyalty: 10},
	}, nil)

	if err := service.CompleteBookings(context.Background()); err != nil {
		t.Fatalf("complete bookings: %v", err)
	}
	if len(repo.completeCalls) != 1 {
		t.Fatalf("expected one complete call")
	}
	if repo.completeCalls[0].visitsForLoyalty != 10 {
		t.Fatalf("expected visits_for_loyalty=10, got %d", repo.completeCalls[0].visitsForLoyalty)
	}
}

func TestBuildLoyaltyStatusIntegration(t *testing.T) {
	status := domain.BuildLoyaltyStatus(10, 10)
	if !status.IsLoyalClient {
		t.Fatal("expected loyal badge after completing enough visits")
	}
}
