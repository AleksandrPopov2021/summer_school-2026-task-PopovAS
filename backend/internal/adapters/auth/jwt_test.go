package jwtauth_test

import (
	"testing"

	jwtauth "github.com/vertical-climbing/backend/internal/adapters/auth"
)

func TestJWTRoundtrip(t *testing.T) {
	service, err := jwtauth.NewService("test-secret")
	if err != nil {
		t.Fatalf("new service: %v", err)
	}

	clientID := "a0000000-0000-4000-8000-000000000099"
	token, err := service.Issue(clientID)
	if err != nil {
		t.Fatalf("issue token: %v", err)
	}

	parsedID, err := service.Parse(token)
	if err != nil {
		t.Fatalf("parse token: %v", err)
	}
	if parsedID != clientID {
		t.Fatalf("expected %s, got %s", clientID, parsedID)
	}
}

func TestJWTInvalidToken(t *testing.T) {
	service, err := jwtauth.NewService("test-secret")
	if err != nil {
		t.Fatalf("new service: %v", err)
	}

	if _, err := service.Parse("invalid.token.value"); err == nil {
		t.Fatalf("expected error for invalid token")
	}
}
