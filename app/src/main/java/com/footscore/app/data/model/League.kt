package com.footscore.app.data.model

/** A lista de ligas vem do backend (GET /leagues) — ver backend/src/leagues.js. */
data class League(
    val slug: String,
    val label: String
)
