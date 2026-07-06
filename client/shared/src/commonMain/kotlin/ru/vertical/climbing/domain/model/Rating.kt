package ru.vertical.climbing.domain.model

import kotlinx.datetime.Instant

/** Оценка инструктора (OpenAPI: InstructorRating, Post-MVP, FR-029). */
data class InstructorRating(
    val id: String,
    val clientId: String,
    val instructorId: String,
    val bookingId: String,
    val stars: Int,
    val ratedAt: Instant,
)

/** Запрос на создание оценки (OpenAPI: CreateRatingRequest, BR-033). */
data class CreateRatingCommand(
    val bookingId: String,
    val stars: Int,
)
