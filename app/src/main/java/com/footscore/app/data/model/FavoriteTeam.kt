package com.footscore.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class FavoriteTeam(
    val id: Int,
    val name: String,
    val logoUrl: String,
    val leagueId: Int
)
