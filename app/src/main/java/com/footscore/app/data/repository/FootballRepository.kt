package com.footscore.app.data.repository

import com.footscore.app.data.remote.FootballApiService
import com.footscore.app.data.remote.dto.FixtureItemDto
import com.footscore.app.data.remote.dto.TeamInfoDto

class FootballRepository(private val api: FootballApiService) {

    suspend fun getFixturesByDate(date: String): List<FixtureItemDto> =
        api.getFixtures(date).response

    suspend fun getTeamsByLeague(leagueId: Int, season: Int): List<TeamInfoDto> =
        api.getTeams(leagueId, season).response.map { it.team }
}
