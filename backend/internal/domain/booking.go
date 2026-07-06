package domain

import (
	portslot "github.com/vertical-climbing/backend/internal/ports/slot"
)

const (
	BookingStatusBooked             = "booked"
	BookingStatusCancelledByClient  = "cancelled_by_client"
	BookingStatusCancelledByGym     = "cancelled_by_gym"
	BookingStatusCompleted          = "completed"
	BookingStatusNoShow             = "no_show"

	ErrorCodeRiskConsentRequired         ErrorCode = "RISK_CONSENT_REQUIRED"
	ErrorCodeInstructorClearanceRequired ErrorCode = "INSTRUCTOR_CLEARANCE_REQUIRED"
	ErrorCodeNoFreeSpots                 ErrorCode = "NO_FREE_SPOTS"
	ErrorCodeBookingCutoffExceeded       ErrorCode = "BOOKING_CUTOFF_EXCEEDED"
	ErrorCodeRebookingForbidden          ErrorCode = "REBOOKING_FORBIDDEN"
	ErrorCodeRentalUnavailable           ErrorCode = "RENTAL_UNAVAILABLE"
	ErrorCodeBookingConflict             ErrorCode = "BOOKING_CONFLICT"
	ErrorCodeCancellationForbidden       ErrorCode = "CANCELLATION_FORBIDDEN"
	ErrorCodeBookingNotCancellable       ErrorCode = "BOOKING_NOT_CANCELLABLE"
	ErrorCodeBookingRentalUpdateForbidden ErrorCode = "BOOKING_RENTAL_UPDATE_FORBIDDEN"
	ErrorCodeBookingForbiddenSanctions    ErrorCode = "BOOKING_FORBIDDEN_SANCTIONS"
)

func NewForbidden(code ErrorCode, message string) *AppError {
	return &AppError{
		Code:    code,
		Message: message,
		Status:  403,
	}
}

func NewUnprocessable(code ErrorCode, message string) *AppError {
	return &AppError{
		Code:    code,
		Message: message,
		Status:  422,
	}
}

func NewRiskConsentRequired() *AppError {
	return NewForbidden(
		ErrorCodeRiskConsentRequired,
		"Необходимо подтвердить согласие на риск",
	)
}

func NewInstructorClearanceRequired() *AppError {
	return NewForbidden(
		ErrorCodeInstructorClearanceRequired,
		"Для записи на тренировки «трассы с верёвкой» требуется допуск инструктора",
	)
}

func NewNoFreeSpots() *AppError {
	return NewUnprocessable(
		ErrorCodeNoFreeSpots,
		"Нет свободных мест на выбранный слот",
	)
}

func NewBookingCutoffExceeded() *AppError {
	return NewUnprocessable(
		ErrorCodeBookingCutoffExceeded,
		"Запись закрыта — до начала тренировки менее 30 минут",
	)
}

func NewRebookingForbidden() *AppError {
	return NewUnprocessable(
		ErrorCodeRebookingForbidden,
		"Повторная запись на этот слот запрещена",
	)
}

func NewRentalUnavailable() *AppError {
	return NewUnprocessable(
		ErrorCodeRentalUnavailable,
		"Выбранные позиции проката недоступны на данный слот",
	)
}

func NewCancellationForbidden() *AppError {
	return NewForbidden(
		ErrorCodeCancellationForbidden,
		"Отмена запрещена менее чем за 1 час до начала тренировки",
	)
}

func NewBookingNotCancellable() *AppError {
	return NewConflict(
		ErrorCodeBookingNotCancellable,
		"Запись уже отменена или завершена",
	)
}

func NewBookingRentalUpdateForbidden() *AppError {
	return NewForbidden(
		ErrorCodeBookingRentalUpdateForbidden,
		"Изменение проката недоступно для этой записи",
	)
}

func NewBookingForbiddenSanctions() *AppError {
	return NewForbidden(
		ErrorCodeBookingForbiddenSanctions,
		"Запись временно недоступна из-за нарушений правил посещения",
	)
}

// BookingConflictError is returned when concurrent booking fails (409).
type BookingConflictError struct {
	Code         ErrorCode
	Message      string
	SlotDetail   portslot.SlotDetail
	Availability BookingAvailability
}

func (e *BookingConflictError) Error() string {
	return string(e.Code) + ": " + e.Message
}

func NewBookingConflict(slotDetail portslot.SlotDetail, availability BookingAvailability) *BookingConflictError {
	return &BookingConflictError{
		Code:         ErrorCodeBookingConflict,
		Message:      "Не удалось забронировать слот — места заняты или слот изменился",
		SlotDetail:   slotDetail,
		Availability: availability,
	}
}
