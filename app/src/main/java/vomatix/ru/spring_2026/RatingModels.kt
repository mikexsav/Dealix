package vomatix.ru.spring_2026

data class RatingMetric(
    val title: String,
    val valueText: String,
    val points: Int,
    val howCalculated: String,
    val howToIncrease: String
)

data class RatingScreenState(
    val totalPoints: Int,
    val currentStatus: String,
    val nextStatus: String,
    val pointsToNextLevel: Int,
    val progressPercent: Int,
    val forecastIncomeText: String,
    val forecastMortgageText: String,
    val metrics: List<RatingMetric>
)