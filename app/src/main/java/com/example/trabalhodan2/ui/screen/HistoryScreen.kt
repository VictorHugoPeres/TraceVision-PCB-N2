package com.example.trabalhodan2.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trabalhodan2.data.model.InferenceResult
import com.example.trabalhodan2.ui.components.StatusBadge
import com.example.trabalhodan2.ui.components.TechnicalCard
import com.example.trabalhodan2.ui.theme.TechBorder
import com.example.trabalhodan2.ui.theme.TechPrimary
import com.example.trabalhodan2.ui.theme.TechSuccess
import com.example.trabalhodan2.ui.theme.TechSurfaceVariant
import com.example.trabalhodan2.viewmodel.InferenceViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: InferenceViewModel,
    onNavigateToDetails: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val filteredList = remember(uiState.historyList, searchQuery) {
        if (searchQuery.isBlank()) {
            uiState.historyList
        } else {
            uiState.historyList.filter {
                it.sessionId.contains(searchQuery, ignoreCase = true) ||
                it.modelName.contains(searchQuery, ignoreCase = true) ||
                it.timestamp.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val totalInferences = uiState.historyList.size
    val totalObjectsCounted = uiState.historyList.sumOf { it.totalObjects }
    val avgExecutionTime = if (totalInferences > 0) {
        uiState.historyList.map { it.executionTimeMs }.average().toLong()
    } else {
        0L
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Histórico de Inferências", fontWeight = FontWeight.Bold, fontSize = 19.sp)
                        Text(
                            if (uiState.isBackendOnline) "● Sincronizado com o Servidor Backend" else "○ Modo Offline (Local)",
                            fontSize = 11.sp,
                            color = if (uiState.isBackendOnline) TechSuccess else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.fetchHistoryFromServer() }) {
                        Icon(Icons.Default.CloudSync, contentDescription = "Sincronizar", tint = TechPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Mini Dashboard com 3 métricas consolidadas (Fidelidade à Figura 4a)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryCard(modifier = Modifier.weight(1f), value = "$totalInferences", label = "INFERÊNCIAS")
                SummaryCard(modifier = Modifier.weight(1.3f), value = "$totalObjectsCounted", label = "OBJETOS CONTADOS")
                SummaryCard(modifier = Modifier.weight(1.1f), value = "${avgExecutionTime} ms", label = "MÉDIA TEMPO")
            }

            // Campo de busca / filtro (Fidelidade à Figura 4a)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Filtrar por data ou modelo...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedBorderColor = TechBorder,
                    focusedBorderColor = TechPrimary
                )
            )

            Text(
                "Registros Recentes (Toque para detalhes)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Lista cronológica de sessões
            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
                        Text("Nenhuma auditoria encontrada.", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredList) { item ->
                        HistoryCardItem(
                            item = item,
                            onClick = {
                                viewModel.selectHistoryItem(item)
                                onNavigateToDetails()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryCard(modifier: Modifier = Modifier, value: String, label: String) {
    Surface(
        modifier = modifier,
        color = TechPrimary,
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.85f),
                letterSpacing = 0.2.sp
            )
        }
    }
}

@Composable
fun HistoryCardItem(item: InferenceResult, onClick: () -> Unit) {
    TechnicalCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Linha 1: ID da Sessão + Data/Hora
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Sessão #${item.sessionId}",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = item.timestamp,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TechPrimary
                )
            }

            // Linha 2: Grid técnico (Modelo, Total, Tempo)
            Surface(
                color = TechSurfaceVariant,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Modelo:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(item.modelName, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Column {
                        Text("Total:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${item.totalObjects} objs", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Column {
                        Text("Tempo:", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${item.executionTimeMs} ms", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Linha 3: Status de Envio + Link Ver Detalhes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusBadge(
                    text = if (item.syncedWithBackend) "✔ Enviado ao Servidor" else "⏳ Pendente",
                    containerColor = if (item.syncedWithBackend) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                    contentColor = if (item.syncedWithBackend) TechSuccess else Color(0xFFD97706)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Ver detalhes",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TechPrimary
                    )
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = TechPrimary
                    )
                }
            }
        }
    }
}
