package domain_test

import (
	"testing"

	"github.com/vertical-climbing/backend/internal/domain"
)

func TestValidatePhone(t *testing.T) {
	tests := []struct {
		name    string
		phone   string
		wantErr bool
	}{
		{name: "valid russian", phone: "+79001234567"},
		{name: "valid us", phone: "+12025550123"},
		{name: "missing plus", phone: "79001234567", wantErr: true},
		{name: "too short", phone: "+123456", wantErr: true},
		{name: "starts with zero", phone: "+09001234567", wantErr: true},
		{name: "letters", phone: "+7900PHONE", wantErr: true},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			err := domain.ValidatePhone(tt.phone)
			if tt.wantErr && err == nil {
				t.Fatalf("expected error")
			}
			if !tt.wantErr && err != nil {
				t.Fatalf("unexpected error: %v", err)
			}
		})
	}
}

func TestValidateRiskConsentUpdate(t *testing.T) {
	falseValue := false
	trueValue := true

	if err := domain.ValidateRiskConsentUpdate(nil); err == nil {
		t.Fatalf("expected error for nil")
	}
	if err := domain.ValidateRiskConsentUpdate(&falseValue); err == nil {
		t.Fatalf("expected error for false")
	}
	if err := domain.ValidateRiskConsentUpdate(&trueValue); err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
}

func TestValidateNotificationPreferencesUpdate(t *testing.T) {
	enabled := true

	if err := domain.ValidateNotificationPreferencesUpdate(nil, nil, nil, nil); err == nil {
		t.Fatalf("expected error for empty update")
	}
	if err := domain.ValidateNotificationPreferencesUpdate(&enabled, nil, &enabled, nil); err == nil {
		t.Fatalf("expected error for reminders_enabled")
	}
	if err := domain.ValidateNotificationPreferencesUpdate(&enabled, nil, nil, &enabled); err == nil {
		t.Fatalf("expected error for gym_cancellation_enabled")
	}
	if err := domain.ValidateNotificationPreferencesUpdate(&enabled, nil, nil, nil); err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
}

func TestBookingForbiddenBySanctions(t *testing.T) {
	if domain.BookingForbiddenBySanctions(2, 0, 3) {
		t.Fatal("expected below threshold")
	}
	if !domain.BookingForbiddenBySanctions(2, 1, 3) {
		t.Fatal("expected sanctions at threshold")
	}
	if !domain.BookingForbiddenBySanctions(5, 5, 3) {
		t.Fatal("expected sanctions above threshold")
	}
}
