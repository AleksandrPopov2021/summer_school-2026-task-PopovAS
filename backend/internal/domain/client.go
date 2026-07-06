package domain

import (
	"regexp"
	"strings"
	"time"
	"unicode/utf8"
)

var e164PhonePattern = regexp.MustCompile(`^\+[1-9]\d{6,14}$`)

func ValidatePhone(phone string) error {
	if !e164PhonePattern.MatchString(phone) {
		return NewBadRequest("Номер телефона должен быть в формате E.164")
	}
	return nil
}

func ValidateFullName(fullName string) error {
	fullName = strings.TrimSpace(fullName)
	length := utf8.RuneCountInString(fullName)
	if length < 1 || length > 200 {
		return NewBadRequest("ФИО должно содержать от 1 до 200 символов")
	}
	return nil
}

func ParseBirthDate(value string) (time.Time, error) {
	birthDate, err := time.Parse("2006-01-02", value)
	if err != nil {
		return time.Time{}, NewBadRequest("Некорректная дата рождения")
	}
	return birthDate, nil
}

func ValidateRiskConsentUpdate(accepted *bool) error {
	if accepted == nil {
		return NewBadRequest("Необходимо указать risk_consent_accepted")
	}
	if !*accepted {
		return NewBadRequest("Можно подтвердить только согласие на риск (true)")
	}
	return nil
}

func ValidateNotificationPreferencesUpdate(
	bookingConfirmation *bool,
	ratingInvitation *bool,
	reminders *bool,
	gymCancellation *bool,
) error {
	if bookingConfirmation == nil && ratingInvitation == nil && reminders == nil && gymCancellation == nil {
		return NewBadRequest("Необходимо указать хотя бы одно поле для обновления")
	}
	if reminders != nil {
		return NewBadRequest("Настройку reminders_enabled изменить нельзя")
	}
	if gymCancellation != nil {
		return NewBadRequest("Настройку gym_cancellation_enabled изменить нельзя")
	}
	return nil
}

func BookingForbiddenBySanctions(lateCancellationCount, noShowCount, violationsForSanctions int32) bool {
	return lateCancellationCount+noShowCount >= violationsForSanctions
}
