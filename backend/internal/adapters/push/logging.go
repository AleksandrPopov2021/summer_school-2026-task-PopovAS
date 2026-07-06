package push

import (
	"context"
	"log/slog"

	portpush "github.com/vertical-climbing/backend/internal/ports/push"
	"github.com/vertical-climbing/backend/internal/domain"
)

type LoggingSender struct{}

func NewLoggingSender() *LoggingSender {
	return &LoggingSender{}
}

func (s *LoggingSender) Send(_ context.Context, clientID string, tokens []portpush.DeviceToken, payload domain.PushPayload) error {
	slog.Info(
		"push notification",
		"client_id", clientID,
		"token_count", len(tokens),
		"type", payload.Type,
		"booking_id", payload.BookingID,
		"slot_id", payload.SlotID,
		"reminder_kind", payload.ReminderKind,
	)
	return nil
}

var _ portpush.Sender = (*LoggingSender)(nil)
