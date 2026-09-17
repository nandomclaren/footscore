package com.footscore.app.data.model

import java.time.LocalDate

/**
 * IDs de liga conforme a API-Football (RapidAPI). Confira/ajuste em
 * https://www.api-football.com/documentation-v3#tag/Leagues caso a API mude algum ID.
 */
data class League(
    val id: Int,
    val displayName: String,
    val isEuropeanSeasonFormat: Boolean = true
)

val MAIN_LEAGUES = listOf(
    League(id = 71, displayName = "Brasileirão Série A", isEuropeanSeasonFormat = false),
    League(id = 39, displayName = "Premier League"),
    League(id = 140, displayName = "La Liga"),
    League(id = 2, displayName = "Champions League"),
    League(id = 135, displayName = "Serie A (Itália)"),
    League(id = 78, displayName = "Bundesliga"),
    League(id = 61, displayName = "Ligue 1")
)

/**
 * Ligas europeias numeram a temporada pelo ano de início (ex.: 2025 para 2025/26).
 * O Brasileirão numeia pelo próprio ano civil.
 */
fun League.currentSeason(today: LocalDate = LocalDate.now()): Int {
    if (!isEuropeanSeasonFormat) return today.year
    return if (today.monthValue >= 7) today.year else today.year - 1
}
