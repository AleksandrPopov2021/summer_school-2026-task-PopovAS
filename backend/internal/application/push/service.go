package push

import (
	"context"
	"log/slog"

	"github.com/vertical-climbing/backend/internal/domain"
	portdevice "github.com/vertical-climbing/backend/internal/ports/device"
	portnotification "github.com/vertical-climbing/backend/internal/ports/notification"
	portpush "github.com/vertical-climbing/backend/internal/ports/push"
	portworker "github.com/vertical-climbing/backend/internal/ports/worker"
)

type Service struct {
	sender        portpush.Sender
	devices       portdevice.Repository
	notifications portnotification.Repository
	worker        portworker.Repository
}

func NewService(
	sender portpush.Sender,
	devices portdevice.Repository,
	notifications portnotification.Repository,
	worker portworker.Repository,
) *Service {
	return &Service{
		sender:        sender,
		devices:       devices,
		notifications: notifications,
		worker:        worker,
	}
}

func (s *Service) NotifyBookingCreated(ctx context.Context, clientID, bookingID, slotID string) {
	prefs, err := s.notifications.GetByClientID(ctx, clientID)
	if err != nil {
		slog.Warn("load notification preferences for booking confirmation", "error", err, "client_id", clientID)
		return
	}
	if !prefs.BookingConfirmationEnabled {
		return
	}

	payload := domain.PushPayload{
		Type:      domain.PushTypeBookingConfirmed,
		BookingID: bookingID,
		SlotID:    slotID,
	}
	if err := s.deliver(ctx, clientID, payload, domain.SentPushBookingConfirmed, bookingID); err != nil {
		slog.Warn("send booking confirmation push", "error", err, "booking_id", bookingID)
	}
}

func (s *Service) NotifyGymCancellation(ctx context.Context, clientID, bookingID, slotID string) {
	payload := domain.PushPayload{
		Type:      domain.PushTypeGymCancellation,
		BookingID: bookingID,
		SlotID:    slotID,
	}
	if err := s.deliver(ctx, clientID, payload, domain.SentPushGymCancellation, bookingID); err != nil {
		slog.Warn("send gym cancellation push", "error", err, "booking_id", bookingID)
	}
}

func (s *Service) SendDayBeforeReminder(ctx context.Context, clientID, bookingID, slotID string) error {
	payload := domain.PushPayload{
		Type:         domain.PushTypeReminder,
		BookingID:    bookingID,
		SlotID:       slotID,
		ReminderKind: domain.ReminderKindDayBefore,
	}
	return s.deliver(ctx, clientID, payload, domain.SentPushReminderDayBefore, bookingID)
}

func (s *Service) SendHoursBeforeReminder(ctx context.Context, clientID, bookingID, slotID string) error {
	payload := domain.PushPayload{
		Type:         domain.PushTypeReminder,
		BookingID:    bookingID,
		SlotID:       slotID,
		ReminderKind: domain.ReminderKindHoursBefore,
	}
	return s.deliver(ctx, clientID, payload, domain.SentPushReminderHoursBefore, bookingID)
}

func (s *Service) SendRatingInvitation(ctx context.Context, clientID, bookingID, slotID string) error {
	prefs, err := s.notifications.GetByClientID(ctx, clientID)
	if err != nil {
		return err
	}
	if !prefs.RatingInvitationEnabled {
		return nil
	}

	payload := domain.PushPayload{
		Type:      domain.PushTypeRatingInvitation,
		BookingID: bookingID,
		SlotID:    slotID,
	}
	return s.deliver(ctx, clientID, payload, domain.SentPushRatingInvitation, bookingID)
}

func (s *Service) deliver(
	ctx context.Context,
	clientID string,
	payload domain.PushPayload,
	sentType string,
	bookingID string,
) error {
	tokens, err := s.devices.ListTokensByClientID(ctx, clientID)
	if err != nil {
		return err
	}
	if len(tokens) == 0 {
		return nil
	}

	deviceTokens := make([]portpush.DeviceToken, 0, len(tokens))
	for _, token := range tokens {
		deviceTokens = append(deviceTokens, portpush.DeviceToken{
			Token:    token.Token,
			Platform: token.Platform,
		})
	}

	if err := s.sender.Send(ctx, clientID, deviceTokens, payload); err != nil {
		return err
	}

	return s.worker.RecordSentPush(ctx, bookingID, sentType)
}

var _ portpush.Notifier = (*Service)(nil)
