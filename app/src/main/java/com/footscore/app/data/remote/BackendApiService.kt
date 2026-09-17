package com.footscore.app.data.remote

import com.footscore.app.data.remote.dto.RegisterDeviceRequest
import com.footscore.app.data.remote.dto.RegisterDeviceResponse
import com.footscore.app.data.remote.dto.TeamsResponseDto
import com.footscore.app.data.remote.dto.TestNotificationRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface BackendApiService {

    @POST("register")
    suspend fun registerDevice(@Body request: RegisterDeviceRequest): RegisterDeviceResponse

    @GET("teams")
    suspend fun getTeams(@Query("tournamentId") tournamentId: Int): TeamsResponseDto

    @POST("test-notify")
    suspend fun sendTestNotification(@Body request: TestNotificationRequest): RegisterDeviceResponse
}
