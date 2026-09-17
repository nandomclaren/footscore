package com.footscore.app.data.repository

import com.footscore.app.data.remote.BackendApiService
import com.footscore.app.data.remote.dto.TeamInfoDto

class FootballRepository(private val api: BackendApiService) {

    suspend fun getTeamsByLeague(leagueId: Int): List<TeamInfoDto> =
        api.getTeams(leagueId).teams
}
