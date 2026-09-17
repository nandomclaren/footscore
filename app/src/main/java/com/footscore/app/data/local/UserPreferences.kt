package com.footscore.app.data.local

import com.footscore.app.data.model.FavoriteTeam

data class UserPreferences(
    val favorites: List<FavoriteTeam> = emptyList(),
    val hour: Int = 20,
    val minute: Int = 0,
    val notifyIfNoGames: Boolean = false
)
