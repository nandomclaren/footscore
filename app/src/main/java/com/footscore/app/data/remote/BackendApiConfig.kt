package com.footscore.app.data.remote

import com.footscore.app.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Aponta para o backend próprio (hospedado no Railway), que esconde a chave
 * da API-Football do app e dispara as notificações via FCM. Configure a URL
 * e o segredo compartilhado em local.properties (backend.baseUrl / backend.secret) —
 * o mesmo segredo precisa estar em APP_SHARED_SECRET no Railway.
 */
object BackendApiConfig {
    private val json = Json { ignoreUnknownKeys = true }

    private val authInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("X-App-Secret", BuildConfig.APP_SHARED_SECRET)
            .build()
        chain.proceed(request)
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(
                    HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
                )
            }
        }
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BACKEND_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val backendApi: BackendApiService by lazy { retrofit.create(BackendApiService::class.java) }
}
