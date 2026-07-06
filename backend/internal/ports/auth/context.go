package auth

import "context"

type TokenService interface {
	Issue(clientID string) (string, error)
	Parse(token string) (clientID string, err error)
}

type contextKey string

const ClientIDKey contextKey = "client_id"

func ClientIDFromContext(ctx context.Context) (string, bool) {
	clientID, ok := ctx.Value(ClientIDKey).(string)
	return clientID, ok && clientID != ""
}

func WithClientID(ctx context.Context, clientID string) context.Context {
	return context.WithValue(ctx, ClientIDKey, clientID)
}
