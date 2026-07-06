package domain_test

import (
	"testing"

	"github.com/vertical-climbing/backend/internal/domain"
)

func TestValidatePushToken(t *testing.T) {
	if err := domain.ValidatePushToken("abc", "android"); err != nil {
		t.Fatalf("expected valid android token: %v", err)
	}
	if err := domain.ValidatePushToken("", "android"); err == nil {
		t.Fatal("expected error for empty token")
	}
	if err := domain.ValidatePushToken("abc", "windows"); err == nil {
		t.Fatal("expected error for invalid platform")
	}
}
