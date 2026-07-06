package domain

import "time"

const RatingWindowHours = 48

const (
	ErrorCodeRatingNotAllowedGymCancelled ErrorCode = "RATING_NOT_ALLOWED_GYM_CANCELLED"
	ErrorCodeRatingWindowExpired          ErrorCode = "RATING_WINDOW_EXPIRED"
	ErrorCodeRatingAlreadySubmitted       ErrorCode = "RATING_ALREADY_SUBMITTED"
)

func ValidateRatingStars(stars int32) error {
	if stars < 1 || stars > 5 {
		return NewBadRequest("Оценка должна быть от 1 до 5 звёзд")
	}
	return nil
}

func RatingWindowExpired(slotStartsAt time.Time, durationMinutes int32, now time.Time) bool {
	slotEnd := slotStartsAt.Add(time.Duration(durationMinutes) * time.Minute)
	deadline := slotEnd.Add(RatingWindowHours * time.Hour)
	return now.After(deadline)
}

func NewRatingNotAllowedGymCancelled() *AppError {
	return NewForbidden(
		ErrorCodeRatingNotAllowedGymCancelled,
		"Оценка недоступна — тренировка была отменена скалодромом",
	)
}

func NewRatingWindowExpired() *AppError {
	return NewForbidden(
		ErrorCodeRatingWindowExpired,
		"Срок оценки истёк (1–2 суток после тренировки)",
	)
}

func NewRatingAlreadySubmitted() *AppError {
	return NewConflict(
		ErrorCodeRatingAlreadySubmitted,
		"Оценка уже была отправлена",
	)
}
