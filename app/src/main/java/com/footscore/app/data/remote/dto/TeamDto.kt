package com.footscore.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class TeamsResponseDto(
    val response: List<TeamEntryDto> = emptyList()
)

@Serializable
data class TeamEntryDto(
    val team: TeamInfoDto
)

@Serializable
data class TeamInfoDto(
    val id: Int,
    val name: String,
    val logo: String? = null,
    val country: String? = null
)
