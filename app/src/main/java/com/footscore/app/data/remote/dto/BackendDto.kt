package com.footscore.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterDeviceRequest(
    val fcmToken: String,
    val favoriteTeamIds: List<Int>,
    val hour: Int,
    val minute: Int,
    val timezone: String,
    val notifyIfNoGames: Boolean
)

@Serializable
data class RegisterDeviceResponse(
    val ok: Boolean
)

@Serializable
data class TestNotificationRequest(
    val fcmToken: String
)

@Serializable
data class LeaguesResponseDto(
    val leagues: List<LeagueDto> = emptyList()
)

@Serializable
data class LeagueDto(
    val slug: String,
    val label: String
)
