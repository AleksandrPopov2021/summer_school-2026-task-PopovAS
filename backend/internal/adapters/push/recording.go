package push

import (
	"context"
	"sync"

	portpush "github.com/vertical-climbing/backend/internal/ports/push"
	"github.com/vertical-climbing/backend/internal/domain"
)

type SentMessage struct {
	ClientID string
	Tokens   []portpush.DeviceToken
	Payload  domain.PushPayload
}

type RecordingSender struct {
	mu       sync.Mutex
	Messages []SentMessage
}

func NewRecordingSender() *RecordingSender {
	return &RecordingSender{}
}

func (s *RecordingSender) Send(_ context.Context, clientID string, tokens []portpush.DeviceToken, payload domain.PushPayload) error {
	s.mu.Lock()
	defer s.mu.Unlock()

	copiedTokens := make([]portpush.DeviceToken, len(tokens))
	copy(copiedTokens, tokens)
	s.Messages = append(s.Messages, SentMessage{
		ClientID: clientID,
		Tokens:   copiedTokens,
		Payload:  payload,
	})
	return nil
}

func (s *RecordingSender) Reset() {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.Messages = nil
}

func (s *RecordingSender) CountByType(pushType string) int {
	s.mu.Lock()
	defer s.mu.Unlock()

	count := 0
	for _, msg := range s.Messages {
		if msg.Payload.Type == pushType {
			count++
		}
	}
	return count
}

var _ portpush.Sender = (*RecordingSender)(nil)
