package domain

import "strings"

const (
	PushTypeBookingConfirmed = "booking_confirmed"
	PushTypeReminder         = "reminder"
	PushTypeGymCancellation  = "gym_cancellation"
	PushTypeRatingInvitation = "rating_invitation"

	ReminderKindDayBefore   = "day_before"
	ReminderKindHoursBefore = "hours_before"

	SentPushBookingConfirmed     = "booking_confirmed"
	SentPushReminderDayBefore    = "reminder_day_before"
	SentPushReminderHoursBefore  = "reminder_hours_before"
	SentPushGymCancellation      = "gym_cancellation"
	SentPushRatingInvitation     = "rating_invitation"
)

type PushPayload struct {
	Type                   string
	BookingID              string
	SlotID                 string
	ReminderKind           string
	CancellationReasonCode string
}

func ValidatePushToken(token, platform string) error {
	token = strings.TrimSpace(token)
	if token == "" {
		return NewBadRequest("Необходимо указать token")
	}
	switch platform {
	case "ios", "android":
		return nil
	default:
		return NewBadRequest("platform должен быть ios или android")
	}
}
