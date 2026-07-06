package domain

import "fmt"

// ErrorCode is a machine-readable API error identifier (NFR-003).
type ErrorCode string

const (
	ErrorCodeBadRequest          ErrorCode = "BAD_REQUEST"
	ErrorCodeUnauthorized        ErrorCode = "UNAUTHORIZED"
	ErrorCodeNotFound            ErrorCode = "NOT_FOUND"
	ErrorCodeInternal            ErrorCode = "INTERNAL_ERROR"
	ErrorCodeClientAlreadyExists ErrorCode = "CLIENT_ALREADY_EXISTS"
)

// AppError represents a domain or application error mapped to API responses.
type AppError struct {
	Code    ErrorCode
	Message string
	Status  int
	Details map[string]any
}

func (e *AppError) Error() string {
	return fmt.Sprintf("%s: %s", e.Code, e.Message)
}

func NewNotFound(message string) *AppError {
	return &AppError{
		Code:    ErrorCodeNotFound,
		Message: message,
		Status:  404,
	}
}

func NewInternal(message string) *AppError {
	return &AppError{
		Code:    ErrorCodeInternal,
		Message: message,
		Status:  500,
	}
}

func NewBadRequest(message string) *AppError {
	return &AppError{
		Code:    ErrorCodeBadRequest,
		Message: message,
		Status:  400,
	}
}

func NewUnauthorized(message string) *AppError {
	return &AppError{
		Code:    ErrorCodeUnauthorized,
		Message: message,
		Status:  401,
	}
}

func NewConflict(code ErrorCode, message string) *AppError {
	return &AppError{
		Code:    code,
		Message: message,
		Status:  409,
	}
}

func NewClientAlreadyExists() *AppError {
	return NewConflict(
		ErrorCodeClientAlreadyExists,
		"Клиент с указанным номером телефона уже зарегистрирован",
	)
}
