package com.footscore.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class FixturesResponseDto(
    val response: List<FixtureItemDto> = emptyList()
)

@Serializable
data class FixtureItemDto(
    val fixture: FixtureInfoDto,
    val league: FixtureLeagueDto,
    val teams: FixtureTeamsDto,
    val goals: FixtureGoalsDto
)

@Serializable
data class FixtureInfoDto(
    val id: Long,
    val date: String,
    val status: FixtureStatusDto
)

@Serializable
data class FixtureStatusDto(
    val short: String
)

@Serializable
data class FixtureLeagueDto(
    val id: Int,
    val name: String
)

@Serializable
data class FixtureTeamsDto(
    val home: FixtureTeamDto,
    val away: FixtureTeamDto
)

@Serializable
data class FixtureTeamDto(
    val id: Int,
    val name: String,
    val logo: String? = null
)

@Serializable
data class FixtureGoalsDto(
    val home: Int? = null,
    val away: Int? = null
)
