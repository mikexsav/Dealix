package vomatix.ru.spring_2026.models

data class LeadResponse(
    val _embedded: Embedded
)

data class Embedded(
    val leads: List<Lead>
)