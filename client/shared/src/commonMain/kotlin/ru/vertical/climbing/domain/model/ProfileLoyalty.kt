package ru.vertical.climbing.domain.model

/** UI-состояние блока лояльности на SCR-010 (FR-027, BR-032). */
data class ProfileLoyaltyState(
    val showBadge: Boolean,
    val progress: Float,
    val completedVisits: Int,
    val visitsForLoyalty: Int,
    val remainingVisits: Int,
    val isLoyalClient: Boolean,
    val loyaltyDiscountPercent: Int?,
)

/** Видимость бейджа «Постоянный клиент» (FR-027). */
fun Client.shouldShowLoyaltyBadge(): Boolean = isLoyalClient

/** Расчёт прогресса лояльности для SCR-010. */
fun buildProfileLoyaltyState(client: Client, visitsForLoyalty: Int): ProfileLoyaltyState {
    val threshold = visitsForLoyalty.coerceAtLeast(1)
    val completed = client.completedVisitsCount.coerceAtLeast(0)
    val progress = when {
        client.isLoyalClient -> 1f
        else -> (completed.toFloat() / threshold).coerceIn(0f, 1f)
    }
    val remaining = if (client.isLoyalClient) 0 else (threshold - completed).coerceAtLeast(0)
    return ProfileLoyaltyState(
        showBadge = client.shouldShowLoyaltyBadge(),
        progress = progress,
        completedVisits = completed,
        visitsForLoyalty = threshold,
        remainingVisits = remaining,
        isLoyalClient = client.isLoyalClient,
        loyaltyDiscountPercent = client.loyaltyDiscount?.toInt(),
    )
}
