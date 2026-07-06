package client

import (
	"context"

	"github.com/vertical-climbing/backend/internal/domain"
	portauth "github.com/vertical-climbing/backend/internal/ports/auth"
	portclient "github.com/vertical-climbing/backend/internal/ports/client"
)

type Service struct {
	repo   portclient.Repository
	tokens portauth.TokenService
}

func NewService(repo portclient.Repository, tokens portauth.TokenService) *Service {
	return &Service{repo: repo, tokens: tokens}
}

func (s *Service) Register(ctx context.Context, input portclient.RegisterInput) (portclient.RegisterResult, error) {
	if err := domain.ValidatePhone(input.Phone); err != nil {
		return portclient.RegisterResult{}, err
	}
	if err := domain.ValidateFullName(input.FullName); err != nil {
		return portclient.RegisterResult{}, err
	}

	exists, err := s.repo.ExistsByPhone(ctx, input.Phone)
	if err != nil {
		return portclient.RegisterResult{}, err
	}
	if exists {
		return portclient.RegisterResult{}, domain.NewClientAlreadyExists()
	}

	client, err := s.repo.Register(ctx, input)
	if err != nil {
		return portclient.RegisterResult{}, err
	}

	token, err := s.tokens.Issue(client.ID)
	if err != nil {
		return portclient.RegisterResult{}, domain.NewInternal("Не удалось выпустить токен авторизации")
	}

	return portclient.RegisterResult{
		AccessToken: token,
		Client:      client,
	}, nil
}

func (s *Service) GetCurrent(ctx context.Context, clientID string) (portclient.Client, error) {
	if clientID == "" {
		return portclient.Client{}, domain.NewUnauthorized("Требуется авторизация")
	}
	return s.repo.GetByID(ctx, clientID)
}

func (s *Service) UpdateRiskConsent(ctx context.Context, clientID string, accepted *bool) (portclient.Client, error) {
	if clientID == "" {
		return portclient.Client{}, domain.NewUnauthorized("Требуется авторизация")
	}
	if err := domain.ValidateRiskConsentUpdate(accepted); err != nil {
		return portclient.Client{}, err
	}
	return s.repo.UpdateRiskConsent(ctx, clientID)
}
