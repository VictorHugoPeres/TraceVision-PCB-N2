package com.example.trabalhodan2.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trabalhodan2.ui.components.StatusBadge
import com.example.trabalhodan2.ui.components.TechnicalCard
import com.example.trabalhodan2.ui.theme.TechBorder
import com.example.trabalhodan2.ui.theme.TechPrimary
import com.example.trabalhodan2.ui.theme.TechPrimaryContainer
import com.example.trabalhodan2.ui.theme.TechSuccess
import com.example.trabalhodan2.ui.theme.TechSurfaceVariant
import com.example.trabalhodan2.viewmodel.InferenceViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    viewModel: InferenceViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToDetails: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val result = uiState.currentInferenceResult
    val bitmap = uiState.selectedBitmap
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Resultado da Inspeção", fontWeight = FontWeight.Bold, fontSize = 19.sp)
                        if (result != null) {
                            Text(
                                "Sessão #${result.sessionId}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
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
                actions = {
                    if (result != null) {
                        if (result.syncedWithBackend) {
                            StatusBadge(
                                text = "✔ BD Servidor",
                                containerColor = Color(0xFFDCFCE7),
                                contentColor = TechSuccess
                            )
                        } else {
                            IconButton(onClick = { viewModel.sendResultToBackend(result) }) {
                                Icon(Icons.Default.CloudUpload, contentDescription = "Reenviar", tint = TechPrimary)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        if (result == null || bitmap == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Nenhum resultado de inferência disponível.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Statistics Grid (Fidelidade ao wireframe da Figura 3b)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Bloco 1: Total de Defeitos
                    TechnicalCard(modifier = Modifier.weight(1f)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "${result.totalObjects}",
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold,
                                color = TechPrimary
                            )
                            Text(
                                "Total de Defeitos",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Bloco 2: Confiança Média
                    TechnicalCard(modifier = Modifier.weight(1f)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                String.format(Locale.US, "%.0f%%", result.averageConfidence * 100),
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold,
                                color = TechPrimary
                            )
                            Text(
                                "Confiança Média",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Telemetria técnica de execução
                Surface(
                    color = TechSurfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, TechBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Tempo de execução: ${result.executionTimeMs} ms (Local NPU/CPU)",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        // Resumo das classes detectadas
                        val classSummary = result.boundingBoxes
                            .groupingBy { it.classLabel }
                            .eachCount()
                            .entries
                            .joinToString(", ") { "${it.key}: ${it.value}" }

                        Text(
                            text = if (classSummary.isNotEmpty()) classSummary else "Nenhum defeito detectado",
                            fontSize = 12.sp,
                            color = TechPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Imagem Anotada com Bounding Boxes Canvas Overlay
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Imagem Anotada (Canvas Compose)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, TechBorder, RoundedCornerShape(12.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        val imageBitmap = bitmap.asImageBitmap()
                        val imgWidth = bitmap.width.toFloat()
                        val imgHeight = bitmap.height.toFloat()

                        Box(modifier = Modifier.fillMaxSize()) {
                            Image(
                                bitmap = imageBitmap,
                                contentDescription = "Inference Result PCB",
                                modifier = Modifier.fillMaxSize()
                            )

                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val canvasWidth = size.width
                                val canvasHeight = size.height

                                val scaleX = canvasWidth / imgWidth
                                val scaleY = canvasHeight / imgHeight

                                for (box in result.boundingBoxes) {
                                    val left = box.xMin * scaleX
                                    val top = box.yMin * scaleY
                                    val width = box.widthPx * scaleX
                                    val height = box.heightPx * scaleY
                                    val cx = box.centroidX * scaleX
                                    val cy = box.centroidY * scaleY

                                    val color = getColorForLabel(box.classLabel)

                                    // Desenha o Retângulo da Bounding Box
                                    drawRect(
                                        color = color,
                                        topLeft = Offset(left, top),
                                        size = Size(width, height),
                                        style = Stroke(width = 4f)
                                    )

                                    // Desenha o Centróide (marca central +)
                                    drawLine(
                                        color = Color.White,
                                        start = Offset(cx - 8f, cy),
                                        end = Offset(cx + 8f, cy),
                                        strokeWidth = 3f
                                    )
                                    drawLine(
                                        color = Color.White,
                                        start = Offset(cx, cy - 8f),
                                        end = Offset(cx, cy + 8f),
                                        strokeWidth = 3f
                                    )

                                    // Desenha a tag com o rótulo e confiança
                                    val labelText = "#${box.boxId} ${box.classLabel} ${(box.confidence * 100).toInt()}%"
                                    drawContext.canvas.nativeCanvas.drawText(
                                        labelText,
                                        left + 4f,
                                        maxOf(28f, top - 8f),
                                        android.graphics.Paint().apply {
                                            this.color = android.graphics.Color.YELLOW
                                            this.textSize = 34f
                                            this.isFakeBoldText = true
                                            this.setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Botão de navegação para a Tela 4: Detalhamento Geométrico
                Button(
                    onClick = {
                        viewModel.selectHistoryItem(result)
                        onNavigateToDetails()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TechPrimary)
                ) {
                    Icon(Icons.Default.ListAlt, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Ver Detalhamento Geométrico das Caixas", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

fun getColorForLabel(label: String): Color {
    return when (label.lowercase()) {
        "burn_down", "componente_queimado", "damage" -> Color(0xFFEF4444)      // Vermelho
        "broken_circuit", "trilha_rompida" -> Color(0xFFA855F7)               // Roxo/Magenta
        "missing_parts", "componente_faltando" -> Color(0xFFEAB308)           // Amarelo
        "shifting", "peca_desalinhada" -> Color(0xFF06B6D4)                   // Ciano
        "less_tin", "more_tin", "arranhao" -> Color(0xFF10B981)               // Verde Esmeralda
        else -> Color(0xFF3B82F6)                                            // Azul
    }
}
