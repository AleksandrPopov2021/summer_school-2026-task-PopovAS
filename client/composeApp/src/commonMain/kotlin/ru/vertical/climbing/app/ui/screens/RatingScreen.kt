@file:OptIn(ExperimentalMaterial3Api::class)

package ru.vertical.climbing.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.resources.stringResource
import ru.vertical.climbing.app.navigation.RatingComponent
import ru.vertical.climbing.app.ui.ErrorView
import ru.vertical.climbing.app.ui.LoadingView
import ru.vertical.climbing.app.ui.errorMessageFor
import ru.vertical.climbing.app.ui.format.heroDateLabel
import ru.vertical.climbing.app.ui.format.timeLabel
import ru.vertical.climbing.domain.model.Booking
import ru.vertical.climbing.domain.model.RatingAvailability
import ru.vertical.climbing.presentation.Async
import ru.vertical.climbing.resources.Res
import ru.vertical.climbing.resources.action_later
import ru.vertical.climbing.resources.rating_action_close
import ru.vertical.climbing.resources.rating_action_submit
import ru.vertical.climbing.resources.rating_already_rated
import ru.vertical.climbing.resources.rating_expired_message
import ru.vertical.climbing.resources.rating_gym_cancelled_message
import ru.vertical.climbing.resources.rating_prompt
import ru.vertical.climbing.resources.rating_star_content_description
import ru.vertical.climbing.resources.rating_success
import ru.vertical.climbing.resources.rating_title
import ru.vertical.climbing.resources.rating_unavailable_title

/** SCR-012 — экран оценки инструктора (LOGIC-014, FR-029). */
@Composable
fun RatingScreen(component: RatingComponent) {
    val model by component.model.subscribeAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val thanksMessage = stringResource(Res.string.rating_success)

    model.snackbarError?.let { error ->
        val errorMessage = errorMessageFor(error)
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(errorMessage)
            component.onDismissSnackbar()
        }
    }

    model.successMessage?.let {
        LaunchedEffect(it) {
            snackbarHostState.showSnackbar(thanksMessage)
            component.onSuccessConsumed()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.rating_title)) },
                navigationIcon = {
                    IconButton(onClick = component::onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when (val bookingAsync = model.booking) {
            Async.Idle, Async.Loading -> LoadingView(Modifier.padding(innerPadding))
            is Async.Error -> ErrorView(
                error = bookingAsync.error,
                onRetry = component::onRetry,
                modifier = Modifier.padding(innerPadding),
            )
            is Async.Content -> {
                val availability = model.availability
                if (availability == null || availability is RatingAvailability.NotEligible) {
                    UnavailableRatingContent(
                        message = stringResource(Res.string.rating_unavailable_title),
                        onClose = component::onLater,
                        modifier = Modifier.padding(innerPadding),
                    )
                } else if (availability is RatingAvailability.Available) {
                    AvailableRatingContent(
                        booking = bookingAsync.data,
                        selectedStars = model.selectedStars,
                        isSubmitting = model.isSubmitting,
                        onStarsSelected = component::onStarsSelected,
                        onSubmit = component::onSubmit,
                        onLater = component::onLater,
                        modifier = Modifier.padding(innerPadding),
                    )
                } else {
                    BlockedRatingContent(
                        availability = availability,
                        onClose = component::onLater,
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }
            Async.Empty -> Unit
        }
    }
}

@Composable
private fun AvailableRatingContent(
    booking: Booking,
    selectedStars: Int?,
    isSubmitting: Boolean,
    onStarsSelected: (Int) -> Unit,
    onSubmit: () -> Unit,
    onLater: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BookingSummaryHeader(booking)
        Text(
            text = stringResource(Res.string.rating_prompt),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        StarRatingRow(
            selectedStars = selectedStars,
            enabled = !isSubmitting,
            onStarsSelected = onStarsSelected,
        )
        Button(
            onClick = onSubmit,
            enabled = selectedStars != null && !isSubmitting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
            } else {
                Text(stringResource(Res.string.rating_action_submit))
            }
        }
        TextButton(onClick = onLater) {
            Text(stringResource(Res.string.action_later))
        }
    }
}

@Composable
private fun BlockedRatingContent(
    availability: RatingAvailability,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val message = when (availability) {
        RatingAvailability.GymCancelled -> stringResource(Res.string.rating_gym_cancelled_message)
        RatingAvailability.Expired -> stringResource(Res.string.rating_expired_message)
        is RatingAvailability.AlreadyRated -> stringResource(Res.string.rating_already_rated)
        else -> stringResource(Res.string.rating_unavailable_title)
    }
    UnavailableRatingContent(
        message = message,
        readOnlyStars = (availability as? RatingAvailability.AlreadyRated)?.stars,
        onClose = onClose,
        modifier = modifier,
    )
}

@Composable
private fun UnavailableRatingContent(
    message: String,
    readOnlyStars: Int? = null,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Filled.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp),
        )
        Text(
            text = stringResource(Res.string.rating_unavailable_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        readOnlyStars?.let { stars ->
            StarRatingRow(selectedStars = stars, enabled = false, onStarsSelected = {})
        }
        Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(Res.string.rating_action_close))
        }
    }
}

@Composable
private fun BookingSummaryHeader(booking: Booking) {
    val slot = booking.slot
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "${slot.startsAt.heroDateLabel()}, ${slot.startsAt.timeLabel()}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = slot.zone.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(slot.instructor.fullName, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun StarRatingRow(
    selectedStars: Int?,
    enabled: Boolean,
    onStarsSelected: (Int) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (star in 1..5) {
            val filled = selectedStars != null && star <= selectedStars
            val starLabel = stringResource(Res.string.rating_star_content_description, star)
            Icon(
                imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.StarOutline,
                contentDescription = starLabel,
                tint = if (filled) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(48.dp)
                    .semantics { contentDescription = starLabel }
                    .then(
                        if (enabled) {
                            Modifier.clickable { onStarsSelected(star) }
                        } else {
                            Modifier
                        },
                    ),
            )
        }
    }
}
