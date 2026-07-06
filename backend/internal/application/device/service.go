package device

import (
	"context"

	"github.com/vertical-climbing/backend/internal/domain"
	portauth "github.com/vertical-climbing/backend/internal/ports/auth"
	portdevice "github.com/vertical-climbing/backend/internal/ports/device"
)

type Service struct {
	repo portdevice.Repository
}

func NewService(repo portdevice.Repository) *Service {
	return &Service{repo: repo}
}

func (s *Service) RegisterPushToken(ctx context.Context, token, platform string) error {
	clientID, ok := portauth.ClientIDFromContext(ctx)
	if !ok {
		return domain.NewUnauthorized("Требуется авторизация")
	}

	if err := domain.ValidatePushToken(token, platform); err != nil {
		return err
	}

	return s.repo.UpsertPushToken(ctx, clientID, token, platform)
}
