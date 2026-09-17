package com.footscore.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.footscore.app.data.model.FavoriteTeam
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "footscore_prefs")

/**
 * Única fonte de verdade para as preferências do usuário: times favoritos e horário
 * da notificação diária. A UI e o Worker leem/escrevem sempre por aqui.
 */
class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val FAVORITES_JSON = stringPreferencesKey("favorites_json")
        val HOUR = intPreferencesKey("hour")
        val MINUTE = intPreferencesKey("minute")
        val NOTIFY_IF_NO_GAMES = booleanPreferencesKey("notify_if_no_games")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val favoritesJson = prefs[Keys.FAVORITES_JSON] ?: "[]"
        val favorites = runCatching {
            Json.decodeFromString<List<FavoriteTeam>>(favoritesJson)
        }.getOrDefault(emptyList())

        UserPreferences(
            favorites = favorites,
            hour = prefs[Keys.HOUR] ?: 20,
            minute = prefs[Keys.MINUTE] ?: 0,
            notifyIfNoGames = prefs[Keys.NOTIFY_IF_NO_GAMES] ?: false
        )
    }

    suspend fun saveFavorites(favorites: List<FavoriteTeam>) {
        context.dataStore.edit { it[Keys.FAVORITES_JSON] = Json.encodeToString(favorites) }
    }

    suspend fun saveTime(hour: Int, minute: Int) {
        context.dataStore.edit {
            it[Keys.HOUR] = hour
            it[Keys.MINUTE] = minute
        }
    }

    suspend fun saveNotifyIfNoGames(value: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFY_IF_NO_GAMES] = value }
    }
}
