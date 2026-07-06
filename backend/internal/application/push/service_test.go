package push_test

import (
	"context"
	"testing"
	"time"

	pushadapter "github.com/vertical-climbing/backend/internal/adapters/push"
	"github.com/vertical-climbing/backend/internal/application/push"
	"github.com/vertical-climbing/backend/internal/domain"
	portdevice "github.com/vertical-climbing/backend/internal/ports/device"
	portnotification "github.com/vertical-climbing/backend/internal/ports/notification"
	portworker "github.com/vertical-climbing/backend/internal/ports/worker"
)

type stubNotificationRepo struct {
	prefs portnotification.Preferences
}

func (s *stubNotificationRepo) GetByClientID(_ context.Context, _ string) (portnotification.Preferences, error) {
	return s.prefs, nil
}

func (s *stubNotificationRepo) Update(_ context.Context, _ string, _ portnotification.UpdateInput) (portnotification.Preferences, error) {
	return s.prefs, nil
}

type stubDeviceRepo struct {
	tokens []portdevice.Token
}

func (s *stubDeviceRepo) UpsertPushToken(_ context.Context, _, _, _ string) error {
	return nil
}

func (s *stubDeviceRepo) ListTokensByClientID(_ context.Context, _ string) ([]portdevice.Token, error) {
	return s.tokens, nil
}

type stubWorkerRepo struct {
	recorded []string
}

func (s *stubWorkerRepo) ListBookingsForDayBeforeReminder(_ context.Context, _ time.Time) ([]portworker.ReminderTarget, error) {
	return nil, nil
}

func (s *stubWorkerRepo) ListBookingsForHoursBeforeReminder(_ context.Context, _ time.Time, _ int32) ([]portworker.ReminderTarget, error) {
	return nil, nil
}

func (s *stubWorkerRepo) ListBookingsToComplete(_ context.Context, _ time.Time) ([]portworker.CompleteTarget, error) {
	return nil, nil
}

func (s *stubWorkerRepo) CompleteBooking(_ context.Context, _, _ string, _ int32) error {
	return nil
}

func (s *stubWorkerRepo) RecordSentPush(_ context.Context, bookingID, notificationType string) error {
	s.recorded = append(s.recorded, notificationType)
	return nil
}

func TestNotifyBookingCreated_SkipsWhenDisabled(t *testing.T) {
	recorder := pushadapter.NewRecordingSender()
	service := push.NewService(
		recorder,
		&stubDeviceRepo{tokens: []portdevice.Token{{Token: "tok", Platform: "android"}}},
		&stubNotificationRepo{prefs: portnotification.Preferences{BookingConfirmationEnabled: false}},
		&stubWorkerRepo{},
	)

	service.NotifyBookingCreated(context.Background(), "client-1", "booking-1", "slot-1")

	if recorder.CountByType(domain.PushTypeBookingConfirmed) != 0 {
		t.Fatalf("expected no push when booking confirmation disabled")
	}
}

func TestNotifyBookingCreated_SendsWhenEnabled(t *testing.T) {
	recorder := pushadapter.NewRecordingSender()
	workerRepo := &stubWorkerRepo{}
	service := push.NewService(
		recorder,
		&stubDeviceRepo{tokens: []portdevice.Token{{Token: "tok", Platform: "android"}}},
		&stubNotificationRepo{prefs: portnotification.Preferences{BookingConfirmationEnabled: true}},
		workerRepo,
	)

	service.NotifyBookingCreated(context.Background(), "client-1", "booking-1", "slot-1")

	if recorder.CountByType(domain.PushTypeBookingConfirmed) != 1 {
		t.Fatalf("expected booking confirmation push")
	}
	if len(workerRepo.recorded) != 1 || workerRepo.recorded[0] != domain.SentPushBookingConfirmed {
		t.Fatalf("expected sent push record, got %v", workerRepo.recorded)
	}
}

func TestNotifyGymCancellation_AlwaysSends(t *testing.T) {
	recorder := pushadapter.NewRecordingSender()
	service := push.NewService(
		recorder,
		&stubDeviceRepo{tokens: []portdevice.Token{{Token: "tok", Platform: "ios"}}},
		&stubNotificationRepo{prefs: portnotification.Preferences{BookingConfirmationEnabled: false}},
		&stubWorkerRepo{},
	)

	service.NotifyGymCancellation(context.Background(), "client-1", "booking-1", "slot-1")

	if recorder.CountByType(domain.PushTypeGymCancellation) != 1 {
		t.Fatalf("expected gym cancellation push")
	}
}

func TestSendRatingInvitation_SkipsWhenDisabled(t *testing.T) {
	recorder := pushadapter.NewRecordingSender()
	service := push.NewService(
		recorder,
		&stubDeviceRepo{tokens: []portdevice.Token{{Token: "tok", Platform: "android"}}},
		&stubNotificationRepo{prefs: portnotification.Preferences{RatingInvitationEnabled: false}},
		&stubWorkerRepo{},
	)

	if err := service.SendRatingInvitation(context.Background(), "client-1", "booking-1", "slot-1"); err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if recorder.CountByType(domain.PushTypeRatingInvitation) != 0 {
		t.Fatal("expected no rating invitation when disabled")
	}
}

func TestSendRatingInvitation_SendsWhenEnabled(t *testing.T) {
	recorder := pushadapter.NewRecordingSender()
	service := push.NewService(
		recorder,
		&stubDeviceRepo{tokens: []portdevice.Token{{Token: "tok", Platform: "android"}}},
		&stubNotificationRepo{prefs: portnotification.Preferences{RatingInvitationEnabled: true}},
		&stubWorkerRepo{},
	)

	if err := service.SendRatingInvitation(context.Background(), "client-1", "booking-1", "slot-1"); err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	if recorder.CountByType(domain.PushTypeRatingInvitation) != 1 {
		t.Fatal("expected rating invitation push")
	}
}
