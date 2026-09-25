package com.example.trabalhodan2.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.SquareFoot
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trabalhodan2.data.model.BoundingBox
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
fun DetailScreen(
    viewModel: InferenceViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedItem = uiState.selectedHistoryItem

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (selectedItem != null) "Detecção #${selectedItem.sessionId}" else "Detalhamento Geométrico",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        if (selectedItem != null) {
                            Text(
                                text = formatDisplayTimestamp(selectedItem.timestamp),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        if (selectedItem == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Text("Nenhuma sessão selecionada para auditoria.", fontSize = 14.sp, color = MaterialTheme.colorScheme.outline)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header com as 4 caixas de Telemetria Geral (Fidelidade à Figura 4b)
                TechnicalCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SessionInfoItem(modifier = Modifier.weight(1f), label = "MODELO", value = selectedItem.modelName)
                            SessionInfoItem(modifier = Modifier.weight(1f), label = "TOTAL CAIXAS", value = "${selectedItem.boundingBoxes.size} caixas")
                        }
                        HorizontalDivider(color = TechBorder, thickness = 0.5.dp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SessionInfoItem(modifier = Modifier.weight(1f), label = "TEMPO INFERÊNCIA", value = "${selectedItem.executionTimeMs} ms")
                            SessionInfoItem(
                                modifier = Modifier.weight(1f),
                                label = "PERSISTÊNCIA",
                                value = if (selectedItem.syncedWithBackend) "✔ BD Servidor" else "Pendente",
                                highlightColor = if (selectedItem.syncedWithBackend) TechSuccess else Color(0xFFD97706)
                            )
                        }
                    }
                }

                // Linha de seção: Telemetria das Caixas Delimitadoras
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Grain, contentDescription = null, tint = TechPrimary, modifier = Modifier.size(18.dp))
                        Text(
                            "Telemetria das Caixas Delimitadoras",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        "Exibindo ${selectedItem.boundingBoxes.size} de ${selectedItem.boundingBoxes.size}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Lista de Caixas com auditoria individualizada (Fidelidade à Figura 4b e RF5)
                if (selectedItem.boundingBoxes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Nenhum defeito detectado nesta sessão.", fontSize = 13.sp, color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(selectedItem.boundingBoxes) { box ->
                            BoundingBoxAuditCardWireframe(box = box)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SessionInfoItem(modifier: Modifier = Modifier, label: String, value: String, highlightColor: Color? = null) {
    Column(modifier = modifier) {
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = highlightColor ?: MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun BoundingBoxAuditCardWireframe(box: BoundingBox) {
    TechnicalCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Linha 1: Título e Badge de Confiança
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Caixa #${String.format(Locale.US, "%02d", box.boxId)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        "(${box.classLabel})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = TechPrimary
                    )
                }
                StatusBadge(
                    text = "Confiança: ${(box.confidence * 100).toInt()}%",
                    containerColor = Color(0xFFE0F2FE),
                    contentColor = TechPrimary
                )
            }

            HorizontalDivider(color = TechBorder, thickness = 0.5.dp)

            // Linha 2: Largura e Altura
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("LARGURA (W_px)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${String.format(Locale.US, "%.2f", box.widthPx)} px", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("ALTURA (H_px)", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${String.format(Locale.US, "%.2f", box.heightPx)} px", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Linha 3: Centróide exato com fundo suave em destaque (RF5)
            Surface(
                color = Color(0xFFF0FDF4),
                shape = RoundedCornerShape(6.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "CENT RÓIDE (X_C, Y_C):",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF15803D)
                    )
                    Text(
                        "(${String.format(Locale.US, "%.2f", box.centroidX)}, ${String.format(Locale.US, "%.2f", box.centroidY)}) px",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF15803D)
                    )
                }
            }

            // Linha 4: Área total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("ÁREA TOTAL (A_px = W_px × H_px):", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${String.format(Locale.US, "%.2f", box.areaPx)} px²",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
