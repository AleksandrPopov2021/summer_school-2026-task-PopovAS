package ru.vertical.climbing.domain.usecase

import kotlinx.datetime.Clock
import ru.vertical.climbing.domain.model.Booking
import ru.vertical.climbing.domain.model.BookingStatus
import ru.vertical.climbing.domain.model.CreateRatingCommand
import ru.vertical.climbing.domain.model.InstructorRating
import ru.vertical.climbing.domain.model.RatingAvailability
import ru.vertical.climbing.domain.model.canPromptRating
import ru.vertical.climbing.domain.model.resolveRatingAvailability
import ru.vertical.climbing.domain.repository.BookingRepository
import ru.vertical.climbing.domain.repository.RatingRepository
import ru.vertical.climbing.domain.util.AppResult
import ru.vertical.climbing.domain.util.map
import ru.vertical.climbing.domain.util.onSuccess

/** SCR-012 — загрузка записи и проверка доступности оценки (LOGIC-014). */
class LoadRatingContextUseCase(
    private val getBookingDetail: GetBookingDetailUseCase,
    private val ratingRepository: RatingRepository,
) {
    data class Context(
        val booking: Booking,
        val availability: RatingAvailability,
    )

    suspend operator fun invoke(bookingId: String): AppResult<Context> =
        when (val result = getBookingDetail(bookingId)) {
            is AppResult.Failure -> result
            is AppResult.Success -> {
                val booking = result.value
                val submittedStars = ratingRepository.submittedStarsFor(bookingId)
                AppResult.Success(
                    Context(
                        booking = booking,
                        availability = resolveRatingAvailability(
                            booking = booking,
                            submittedStars = submittedStars,
                            now = Clock.System.now(),
                        ),
                    ),
                )
            }
        }
}

/** LOGIC-014 — отправка оценки инструктора (POST /ratings). */
class SubmitRatingUseCase(
    private val ratingRepository: RatingRepository,
) {
    suspend operator fun invoke(bookingId: String, stars: Int): AppResult<InstructorRating> {
        require(stars in 1..5) { "stars must be 1..5" }
        return ratingRepository.createRating(CreateRatingCommand(bookingId, stars))
            .onSuccess { ratingRepository.markSubmitted(bookingId, stars) }
    }
}

/** SCR-006 — завершённые записи, доступные для оценки. */
class ListRateableBookingsUseCase(
    private val bookingRepository: BookingRepository,
    private val ratingRepository: RatingRepository,
) {
    suspend operator fun invoke(): AppResult<List<Booking>> =
        bookingRepository.listBookings(BookingStatus.COMPLETED).map { bookings ->
            bookings.filter { booking ->
                canPromptRating(
                    booking = booking,
                    submittedStars = ratingRepository.submittedStarsFor(booking.id),
                )
            }
        }
}
