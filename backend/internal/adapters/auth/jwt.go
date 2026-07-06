package jwtauth

import (
	"errors"
	"fmt"
	"time"

	"github.com/golang-jwt/jwt/v5"
	"github.com/vertical-climbing/backend/internal/domain"
)

const tokenTTL = 30 * 24 * time.Hour

type Claims struct {
	ClientID string `json:"client_id"`
	jwt.RegisteredClaims
}

type Service struct {
	secret []byte
}

func NewService(secret string) (*Service, error) {
	if secret == "" {
		return nil, fmt.Errorf("jwt secret is required")
	}
	return &Service{secret: []byte(secret)}, nil
}

func (s *Service) Issue(clientID string) (string, error) {
	now := time.Now()
	claims := Claims{
		ClientID: clientID,
		RegisteredClaims: jwt.RegisteredClaims{
			Subject:   clientID,
			IssuedAt:  jwt.NewNumericDate(now),
			ExpiresAt: jwt.NewNumericDate(now.Add(tokenTTL)),
		},
	}

	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	return token.SignedString(s.secret)
}

func (s *Service) Parse(tokenString string) (string, error) {
	token, err := jwt.ParseWithClaims(tokenString, &Claims{}, func(token *jwt.Token) (any, error) {
		if token.Method != jwt.SigningMethodHS256 {
			return nil, fmt.Errorf("unexpected signing method")
		}
		return s.secret, nil
	})
	if err != nil {
		return "", domain.NewUnauthorized("Требуется авторизация")
	}

	claims, ok := token.Claims.(*Claims)
	if !ok || !token.Valid || claims.ClientID == "" {
		return "", domain.NewUnauthorized("Требуется авторизация")
	}

	return claims.ClientID, nil
}

func IsUnauthorized(err error) bool {
	var appErr *domain.AppError
	return errors.As(err, &appErr) && appErr.Code == domain.ErrorCodeUnauthorized
}
