package com.footscore.app.worker

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

/**
 * WorkManager não suporta agendamento periódico em um horário fixo do relógio,
 * então usamos um OneTimeWorkRequest com o delay exato até o próximo horário
 * configurado; ao terminar, o próprio Worker chama scheduleDaily de novo para
 * encadear o dia seguinte (veja DailySummaryWorker).
 */
object WorkScheduler {
    private const val DAILY_WORK_NAME = "daily_footscore_check"
    private const val TEST_WORK_NAME = "daily_footscore_test"

    fun scheduleDaily(context: Context, hour: Int, minute: Int) {
        val delay = computeInitialDelayMillis(hour, minute)
        val request = OneTimeWorkRequestBuilder<DailySummaryWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            DAILY_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    /** Dispara a checagem imediatamente, útil para testar notificações. */
    fun runNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<DailySummaryWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            TEST_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun computeInitialDelayMillis(hour: Int, minute: Int): Long {
        val now = ZonedDateTime.now()
        var next = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!next.isAfter(now)) {
            next = next.plusDays(1)
        }
        return Duration.between(now, next).toMillis()
    }
}
