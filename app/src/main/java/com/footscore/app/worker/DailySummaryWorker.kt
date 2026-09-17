package com.footscore.app.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.footscore.app.data.local.UserPreferencesRepository
import com.footscore.app.data.remote.ApiConfig
import com.footscore.app.data.remote.dto.FixtureItemDto
import com.footscore.app.data.repository.FootballRepository
import com.footscore.app.notification.NotificationHelper
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.first

/**
 * Consulta a API por jogos de hoje e de ontem, filtra pelos times favoritos e,
 * se houver resultado, dispara a notificação. Ao final, sempre reagenda o
 * próximo disparo (amanhã, no mesmo horário configurado).
 */
class DailySummaryWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefsRepository = UserPreferencesRepository(applicationContext)
        val prefs = prefsRepository.userPreferencesFlow.first()

        if (prefs.favorites.isNotEmpty()) {
            try {
                val repository = FootballRepository(ApiConfig.footballApi)
                val formatter = DateTimeFormatter.ISO_LOCAL_DATE
                val today = LocalDate.now()
                val yesterday = today.minusDays(1)

                val fixturesToday = repository.getFixturesByDate(today.format(formatter))
                val fixturesYesterday = repository.getFixturesByDate(yesterday.format(formatter))

                val favoriteIds = prefs.favorites.map { it.id }.toSet()
                val relevant: List<FixtureItemDto> = (fixturesToday + fixturesYesterday)
                    .filter { fixture ->
                        fixture.fixture.status.short in FINISHED_STATUSES &&
                            (fixture.teams.home.id in favoriteIds || fixture.teams.away.id in favoriteIds)
                    }
                    .distinctBy { it.fixture.id }

                if (relevant.isNotEmpty()) {
                    NotificationHelper.showResultsNotification(applicationContext, relevant)
                } else if (prefs.notifyIfNoGames) {
                    NotificationHelper.showNoGamesNotification(applicationContext)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Falha ao buscar resultados do dia", e)
            }
        }

        // Reencadeia o próximo disparo para o dia seguinte, no horário atual salvo.
        WorkScheduler.scheduleDaily(applicationContext, prefs.hour, prefs.minute)

        return Result.success()
    }

    companion object {
        private const val TAG = "DailySummaryWorker"
        private val FINISHED_STATUSES = setOf("FT", "AET", "PEN")
    }
}
