.PHONY: run test migrate seed generate tidy docker-up docker-down worker lint test-integration test-load staging-up staging-down

DATABASE_URL ?= postgres://vertical:vertical@localhost:5432/vertical?sslmode=disable
export DATABASE_URL

run:
	go run ./cmd/api

worker:
	go run ./cmd/worker

test:
	go test ./...

test-integration:
	go test ./internal/adapters/http -run 'Integration|ClientFlow|Contract' -count=1

test-load:
	go test ./internal/adapters/http -run 'FiftyConcurrent' -count=1

lint:
	golangci-lint run ./...

migrate:
	go run github.com/pressly/goose/v3/cmd/goose@latest -dir migrations postgres "$(DATABASE_URL)" up

migrate-down:
	go run github.com/pressly/goose/v3/cmd/goose@latest -dir migrations postgres "$(DATABASE_URL)" down

seed:
	go run ./cmd/seed

generate: generate-sqlc

generate-api:
	go run github.com/oapi-codegen/oapi-codegen/v2/cmd/oapi-codegen@latest -config oapi-codegen.yaml api/openapi.yaml

generate-sqlc:
	sqlc generate

tidy:
	go mod tidy

docker-up:
	docker compose up -d

docker-down:
	docker compose down

staging-up:
	docker compose -f docker-compose.staging.yml up --build -d

staging-down:
	docker compose -f docker-compose.staging.yml down

setup: docker-up migrate seed
	@echo "Backend ready. Run: make run (and optionally: make worker)"
