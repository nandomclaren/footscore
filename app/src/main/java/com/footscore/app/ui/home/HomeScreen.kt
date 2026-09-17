package com.footscore.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.footscore.app.data.model.MAIN_LEAGUES
import com.footscore.app.ui.home.components.FootscoreTimePickerDialog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showTimePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Footscore") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Times favoritos
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Meus times favoritos", fontWeight = FontWeight.SemiBold)
                if (uiState.favorites.isEmpty()) {
                    Text("Nenhum time selecionado ainda. Escolha abaixo por liga.")
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.favorites.forEach { favorite ->
                            InputChip(
                                selected = true,
                                onClick = { viewModel.removeFavorite(favorite.id) },
                                label = { Text(favorite.name) },
                                trailingIcon = {
                                    Text("×", fontWeight = FontWeight.Bold)
                                },
                                colors = InputChipDefaults.inputChipColors()
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // Escolher por liga
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Escolher por liga", fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MAIN_LEAGUES.forEach { league ->
                        FilterChip(
                            selected = uiState.selectedLeague?.id == league.id,
                            onClick = { viewModel.selectLeague(league) },
                            label = { Text(league.displayName) }
                        )
                    }
                }

                when {
                    uiState.isLoadingTeams -> {
                        Row(
                            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        }
                    }
                    uiState.teamsError != null -> {
                        Text(uiState.teamsError.orEmpty())
                    }
                    uiState.selectedLeague != null && uiState.teams.isNotEmpty() -> {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.teams.forEach { team ->
                                val isFavorite = uiState.favorites.any { it.id == team.id }
                                FilterChip(
                                    selected = isFavorite,
                                    onClick = { viewModel.toggleFavorite(team, uiState.selectedLeague!!) },
                                    label = { Text(team.name) }
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            // Horário da notificação
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Horário da notificação diária", fontWeight = FontWeight.SemiBold)
                OutlinedButton(onClick = { showTimePicker = true }) {
                    Text("%02d:%02d".format(uiState.hour, uiState.minute))
                }
            }

            // Notificar mesmo sem jogos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Avisar mesmo quando não houver jogos")
                Switch(
                    checked = uiState.notifyIfNoGames,
                    onCheckedChange = { viewModel.updateNotifyIfNoGames(it) }
                )
            }

            HorizontalDivider()

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = { viewModel.sendTestNotification() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Testar notificação agora")
            }

            Text(
                text = "A notificação diária está agendada para %02d:%02d, verificando jogos de hoje e de ontem."
                    .format(uiState.hour, uiState.minute),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }

    if (showTimePicker) {
        FootscoreTimePickerDialog(
            initialHour = uiState.hour,
            initialMinute = uiState.minute,
            onDismiss = { showTimePicker = false },
            onConfirm = { hour, minute ->
                viewModel.updateTime(hour, minute)
                showTimePicker = false
            }
        )
    }
}
