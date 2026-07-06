package ru.vertical.climbing.app.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import org.jetbrains.compose.resources.stringResource
import ru.vertical.climbing.app.theme.VerticalTheme
import ru.vertical.climbing.app.ui.EmptyScheduleView
import ru.vertical.climbing.resources.Res
import ru.vertical.climbing.resources.title_schedule

/** Screenshot/preview тесты ключевых экранов (Итерация 9). */
@Preview(showBackground = true, name = "Schedule title")
@Composable
private fun ScheduleTitlePreview() {
    VerticalTheme {
        Text(
            text = stringResource(Res.string.title_schedule),
            style = MaterialTheme.typography.headlineMedium,
        )
    }
}

@Preview(showBackground = true, name = "Empty schedule")
@Composable
private fun EmptySchedulePreview() {
    VerticalTheme {
        EmptyScheduleView()
    }
}

@Preview(showBackground = true, name = "Offline banner context")
@Composable
private fun ScheduleShellPreview() {
    VerticalTheme {
        Text("Вертикаль — MVP preview")
    }
}
