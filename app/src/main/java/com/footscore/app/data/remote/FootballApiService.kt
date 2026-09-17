package com.footscore.app.data.remote

import com.footscore.app.data.remote.dto.FixturesResponseDto
import com.footscore.app.data.remote.dto.TeamsResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface FootballApiService {

    /** Jogos de uma data específica (formato yyyy-MM-dd). */
    @GET("fixtures")
    suspend fun getFixtures(@Query("date") date: String): FixturesResponseDto

    /** Times de uma liga em uma temporada (ano de início da temporada). */
    @GET("teams")
    suspend fun getTeams(
        @Query("league") leagueId: Int,
        @Query("season") season: Int
    ): TeamsResponseDto
}
