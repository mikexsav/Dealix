package vomatix.ru.spring_2026

import kotlin.math.roundToInt

object RatingCalculator {

    fun calculate(
        volumeUnits: Int,
        dealsCount: Int,
        bankSharePercent: Double
    ): RatingScreenState {
        val volumePoints = (volumeUnits * 2).coerceAtMost(40)
        val dealsPoints = dealsCount.coerceAtMost(30)
        val sharePoints = (bankSharePercent * 3).roundToInt().coerceAtMost(30)

        val totalPoints = volumePoints + dealsPoints + sharePoints

        val currentStatus = when {
            totalPoints < 55 -> "Silver"
            totalPoints < 90 -> "Gold"
            else -> "Black"
        }

        val nextStatus = when (currentStatus) {
            "Silver" -> "Gold"
            "Gold" -> "Black"
            else -> "Black"
        }

        val nextThreshold = when (currentStatus) {
            "Silver" -> 55
            "Gold" -> 90
            else -> 100
        }

        val pointsToNext = (nextThreshold - totalPoints).coerceAtLeast(0)
        val progressPercent = ((totalPoints * 100.0) / nextThreshold).roundToInt().coerceIn(0, 100)

        val forecastIncome = when (currentStatus) {
            "Silver" -> "180 000 ₽"
            "Gold" -> "320 000 ₽"
            else -> "500 000 ₽"
        }

        val forecastMortgage = when (currentStatus) {
            "Silver" -> "740 000 ₽"
            "Gold" -> "1 050 000 ₽"
            else -> "1 400 000 ₽"
        }

        return RatingScreenState(
            totalPoints = totalPoints,
            currentStatus = currentStatus,
            nextStatus = nextStatus,
            pointsToNextLevel = pointsToNext,
            progressPercent = progressPercent,
            forecastIncomeText = forecastIncome,
            forecastMortgageText = forecastMortgage,
            metrics = listOf(
                RatingMetric(
                    title = "Объем",
                    valueText = "${volumeUnits} ед.",
                    points = volumePoints,
                    howCalculated = "Считается по объёму профинансированных сделок.",
                    howToIncrease = "Увеличь объём кредитов и средний чек."
                ),
                RatingMetric(
                    title = "Сделки",
                    valueText = "${dealsCount} ед.",
                    points = dealsPoints,
                    howCalculated = "Считается по количеству оформленных сделок.",
                    howToIncrease = "Делай больше успешных оформлений в день."
                ),
                RatingMetric(
                    title = "Доля банка",
                    valueText = "${bankSharePercent.toInt()}%",
                    points = sharePoints,
                    howCalculated = "Считается по доле банка в портфеле.",
                    howToIncrease = "Поднимай долю банка в каждой продаже."
                )
            )
        )
    }
}