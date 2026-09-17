package com.footscore.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class TeamsResponseDto(
    val teams: List<TeamInfoDto> = emptyList()
)

@Serializable
data class TeamInfoDto(
    val id: Int,
    val name: String,
    val logo: String? = null,
    val country: String? = null
)
