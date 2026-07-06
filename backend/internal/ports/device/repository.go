package device

import "context"

type Token struct {
	Token    string
	Platform string
}

type Repository interface {
	UpsertPushToken(ctx context.Context, clientID, token, platform string) error
	ListTokensByClientID(ctx context.Context, clientID string) ([]Token, error)
}
