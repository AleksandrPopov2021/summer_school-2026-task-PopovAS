package push

import (
	"context"

	portpush "github.com/vertical-climbing/backend/internal/ports/push"
	"github.com/vertical-climbing/backend/internal/domain"
)

type NoopSender struct{}

func NewNoopSender() *NoopSender {
	return &NoopSender{}
}

func (s *NoopSender) Send(_ context.Context, _ string, _ []portpush.DeviceToken, _ domain.PushPayload) error {
	return nil
}

var _ portpush.Sender = (*NoopSender)(nil)
