package com.footscore.app.data.repository

import com.footscore.app.data.local.UserPreferences
import com.footscore.app.data.remote.BackendApiService
import com.footscore.app.data.remote.dto.RegisterDeviceRequest
import com.footscore.app.data.remote.dto.TestNotificationRequest
import com.google.firebase.messaging.FirebaseMessaging
import java.util.TimeZone
import kotlinx.coroutines.tasks.await

/**
 * Mantém o backend a par do token do FCM deste aparelho e das preferências
 * atuais (times favoritos, horário, fuso). O backend é quem decide, no
 * horário configurado, se dispara ou não a notificação — o app só registra.
 */
class DeviceRegistrationRepository(private val api: BackendApiService) {

    private suspend fun currentFcmToken(): String = FirebaseMessaging.getInstance().token.await()

    suspend fun registerCurrentDevice(prefs: UserPreferences) {
        if (prefs.favorites.isEmpty()) return
        api.registerDevice(
            RegisterDeviceRequest(
                fcmToken = currentFcmToken(),
                favoriteTeamIds = prefs.favorites.map { it.id },
                hour = prefs.hour,
                minute = prefs.minute,
                timezone = TimeZone.getDefault().id,
                notifyIfNoGames = prefs.notifyIfNoGames
            )
        )
    }

    suspend fun sendTestNotification() {
        api.sendTestNotification(TestNotificationRequest(fcmToken = currentFcmToken()))
    }
}
