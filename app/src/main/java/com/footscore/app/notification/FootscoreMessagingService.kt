package com.footscore.app.notification

import android.util.Log
import com.footscore.app.data.local.UserPreferencesRepository
import com.footscore.app.data.remote.BackendApiConfig
import com.footscore.app.data.repository.DeviceRegistrationRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class FootscoreMessagingService : FirebaseMessagingService() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onNewToken(token: String) {
        // O backend guarda o token junto das preferências; ao trocar, reenviamos
        // o registro completo para não perder o vínculo com os times favoritos.
        scope.launch {
            try {
                val prefs = UserPreferencesRepository(applicationContext).userPreferencesFlow.first()
                DeviceRegistrationRepository(BackendApiConfig.backendApi).registerCurrentDevice(prefs)
            } catch (e: Exception) {
                Log.e(TAG, "Falha ao reenviar token para o backend", e)
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        // O backend sempre manda mensagens "data-only" (sem bloco "notification"),
        // assim este método é chamado tanto em primeiro quanto em segundo plano.
        val title = message.data["title"] ?: return
        val body = message.data["body"] ?: return
        NotificationHelper.showPushNotification(applicationContext, title, body)
    }

    companion object {
        private const val TAG = "FootscoreMessagingService"
    }
}
