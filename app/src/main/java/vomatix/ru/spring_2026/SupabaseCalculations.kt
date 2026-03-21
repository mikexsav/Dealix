package vomatix.ru.spring_2026

data class CalcRequest(
    val fact_volume: Double,
    val plan_volume: Double,
    val fact_deals: Int,
    val plan_deals: Int,
    val fact_share: Double,
    val target_share: Double,
    val approved_apps: Int,
    val total_apps: Int
)

data class CalcResponse(
    val rating: String,
    val status: String,
    val nextLevel: String,
    val pointsToNext: String,
    val incomeBoost: Double,
    val indices: Map<String, Double>
)