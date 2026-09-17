package com.footscore.app.ui.home

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.footscore.app.data.local.UserPreferencesRepository
import com.footscore.app.data.model.FavoriteTeam
import com.footscore.app.data.model.League
import com.footscore.app.data.model.currentSeason
import com.footscore.app.data.remote.BackendApiConfig
import com.footscore.app.data.remote.dto.TeamInfoDto
import com.footscore.app.data.repository.DeviceRegistrationRepository
import com.footscore.app.data.repository.FootballRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val favorites: List<FavoriteTeam> = emptyList(),
    val hour: Int = 20,
    val minute: Int = 0,
    val notifyIfNoGames: Boolean = false,
    val selectedLeague: League? = null,
    val teams: List<TeamInfoDto> = emptyList(),
    val isLoadingTeams: Boolean = false,
    val teamsError: String? = null,
    val registrationError: String? = null
)

private data class LeagueBrowseState(
    val selectedLeague: League? = null,
    val teams: List<TeamInfoDto> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepository = UserPreferencesRepository(application)
    private val footballRepository = FootballRepository(BackendApiConfig.backendApi)
    private val deviceRegistrationRepository = DeviceRegistrationRepository(BackendApiConfig.backendApi)

    private val leagueState = MutableStateFlow(LeagueBrowseState())
    private val registrationErrorState = MutableStateFlow<String?>(null)

    val uiState: StateFlow<HomeUiState> = combine(
        prefsRepository.userPreferencesFlow,
        leagueState,
        registrationErrorState
    ) { prefs, browse, registrationError ->
        HomeUiState(
            favorites = prefs.favorites,
            hour = prefs.hour,
            minute = prefs.minute,
            notifyIfNoGames = prefs.notifyIfNoGames,
            selectedLeague = browse.selectedLeague,
            teams = browse.teams,
            isLoadingTeams = browse.isLoading,
            teamsError = browse.error,
            registrationError = registrationError
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        // Sempre que as preferências mudarem (inclusive na primeira leitura), reenvia
        // o registro para o backend, que é quem decide se/quando notificar.
        viewModelScope.launch {
            prefsRepository.userPreferencesFlow.collect { prefs ->
                try {
                    deviceRegistrationRepository.registerCurrentDevice(prefs)
                    registrationErrorState.value = null
                } catch (e: Exception) {
                    Log.e(TAG, "Falha ao registrar dispositivo no backend", e)
                    registrationErrorState.value = "Não foi possível conectar ao servidor de notificações."
                }
            }
        }
    }

    fun selectLeague(league: League) {
        leagueState.update { it.copy(selectedLeague = league, isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val teams = footballRepository.getTeamsByLeague(league.id, league.currentSeason())
                leagueState.update { it.copy(teams = teams, isLoading = false) }
            } catch (e: Exception) {
                leagueState.update {
                    it.copy(
                        isLoading = false,
                        error = "Não foi possível carregar os times. Verifique o backend e a conexão."
                    )
                }
            }
        }
    }

    fun toggleFavorite(team: TeamInfoDto, league: League) {
        viewModelScope.launch {
            val current = prefsRepository.userPreferencesFlow.first().favorites
            val exists = current.any { it.id == team.id }
            val updated = if (exists) {
                current.filterNot { it.id == team.id }
            } else {
                current + FavoriteTeam(
                    id = team.id,
                    name = team.name,
                    logoUrl = team.logo.orEmpty(),
                    leagueId = league.id
                )
            }
            prefsRepository.saveFavorites(updated)
        }
    }

    fun removeFavorite(teamId: Int) {
        viewModelScope.launch {
            val updated = prefsRepository.userPreferencesFlow.first().favorites.filterNot { it.id == teamId }
            prefsRepository.saveFavorites(updated)
        }
    }

    fun updateTime(hour: Int, minute: Int) {
        viewModelScope.launch { prefsRepository.saveTime(hour, minute) }
    }

    fun updateNotifyIfNoGames(value: Boolean) {
        viewModelScope.launch { prefsRepository.saveNotifyIfNoGames(value) }
    }

    fun sendTestNotification() {
        viewModelScope.launch {
            try {
                deviceRegistrationRepository.sendTestNotification()
            } catch (e: Exception) {
                Log.e(TAG, "Falha ao enviar notificação de teste", e)
                registrationErrorState.value = "Não foi possível enviar a notificação de teste."
            }
        }
    }

    companion object {
        private const val TAG = "HomeViewModel"
    }
}
