package com.footscore.app.data.repository

import com.footscore.app.data.model.League
import com.footscore.app.data.remote.BackendApiService
import com.footscore.app.data.remote.dto.TeamInfoDto

class FootballRepository(private val api: BackendApiService) {

    suspend fun getLeagues(): List<League> =
        api.getLeagues().leagues.map { League(slug = it.slug, label = it.label) }

    suspend fun getTeamsByLeague(slug: String): List<TeamInfoDto> =
        api.getTeams(slug).teams
}
