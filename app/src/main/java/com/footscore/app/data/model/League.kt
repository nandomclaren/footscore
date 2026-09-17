package com.footscore.app.data.model

/**
 * IDs de "uniqueTournament" conforme a API pública do Sofascore. São os números
 * visíveis no final da URL de cada torneio em sofascore.com, ex.:
 * sofascore.com/football/tournament/england/premier-league/17 -> id = 17.
 * O backend resolve a temporada atual sozinho (não precisa mandar daqui).
 */
data class League(
    val id: Int,
    val displayName: String
)

val MAIN_LEAGUES = listOf(
    League(id = 325, displayName = "Brasileirão Série A"),
    League(id = 17, displayName = "Premier League"),
    League(id = 8, displayName = "La Liga"),
    League(id = 7, displayName = "Champions League"),
    League(id = 23, displayName = "Serie A (Itália)"),
    League(id = 35, displayName = "Bundesliga"),
    League(id = 34, displayName = "Ligue 1")
)
