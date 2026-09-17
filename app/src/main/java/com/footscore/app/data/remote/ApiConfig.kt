package com.footscore.app.data.remote

import com.footscore.app.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/**
 * Ponto único de configuração da API-Football (via RapidAPI).
 *
 * A chave gratuita fica em local.properties -> rapidapi.key (veja local.properties.example),
 * lida em build.gradle.kts e exposta aqui como BuildConfig.RAPIDAPI_KEY.
 * Crie sua chave em: https://rapidapi.com/api-sports/api/api-football
 */
object ApiConfig {
    private const val BASE_URL = "https://api-football-v1.p.rapidapi.com/v3/"
    private const val API_HOST = "api-football-v1.p.rapidapi.com"

    private val json = Json { ignoreUnknownKeys = true }

    private val authInterceptor = okhttp3.Interceptor { chain ->
        val request = chain.request().newBuilder()
            .addHeader("X-RapidAPI-Key", BuildConfig.RAPIDAPI_KEY)
            .addHeader("X-RapidAPI-Host", API_HOST)
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
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    val footballApi: FootballApiService by lazy { retrofit.create(FootballApiService::class.java) }
}
