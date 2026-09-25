package com.example.trabalhodan2.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.rememberAsyncImagePainter
import com.example.trabalhodan2.ui.components.StatusBadge
import com.example.trabalhodan2.ui.components.TechnicalCard
import com.example.trabalhodan2.ui.theme.TechBorder
import com.example.trabalhodan2.ui.theme.TechPrimary
import com.example.trabalhodan2.ui.theme.TechPrimaryContainer
import com.example.trabalhodan2.ui.theme.TechSuccess
import com.example.trabalhodan2.ui.theme.TechSurfaceVariant
import com.example.trabalhodan2.viewmodel.InferenceViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigScreen(
    viewModel: InferenceViewModel,
    onNavigateToResult: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    var showServerDialog by remember { mutableStateOf(false) }
    var serverUrlInput by remember { mutableStateOf(uiState.backendUrl) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.setSelectedImage(it, context) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempCameraUri?.let { viewModel.setSelectedImage(it, context) }
        }
    }

    val modelPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = it.lastPathSegment ?: "custom_model.onnx"
            viewModel.loadModelFromUri(it, fileName)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "TraceVision",
                            fontWeight = FontWeight.Bold,
                            fontSize = 19.sp,
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            "Inspeção PCB On-Device & YOLO ONNX",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    Surface(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clickable { showServerDialog = true },
                        shape = RoundedCornerShape(20.dp),
                        color = if (uiState.isBackendOnline) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                        border = BorderStroke(1.dp, if (uiState.isBackendOnline) TechSuccess else Color(0xFFEF4444))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                if (uiState.isBackendOnline) Icons.Default.Cloud else Icons.Default.CloudOff,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (uiState.isBackendOnline) TechSuccess else Color(0xFFDC2626)
                            )
                            Text(
                                text = if (uiState.isBackendOnline) "API Online" else "API Offline",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.isBackendOnline) TechSuccess else Color(0xFFDC2626)
                            )
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Configurar",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // RF1: Modelo YOLO (ONNX)
            TechnicalCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Memory, contentDescription = null, tint = TechPrimary, modifier = Modifier.size(20.dp))
                            Text("Modelo YOLO (ONNX)", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        StatusBadge(
                            text = if (uiState.isModelLoaded) "✔ Ativo" else "Simulado",
                            containerColor = if (uiState.isModelLoaded) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                            contentColor = if (uiState.isModelLoaded) TechSuccess else Color(0xFFD97706)
                        )
                    }

                    Surface(
                        color = TechSurfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = uiState.modelName,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TechPrimary
                            )
                            Text(
                                text = "Entrada: 640x640 BGR | Tensor [1, 11, 8400]",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { modelPickerLauncher.launch("*/*") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, TechBorder)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Selecionar Outro Modelo (.onnx)")
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Rótulos de defeitos suportados (7 classes):",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "broken_circuit, burn_down, damage, less_tin, missing_parts, more_tin, shifting",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // RF2: Entrada de Amostra PCB (Câmera / Galeria)
            TechnicalCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Capturar ou Selecionar Imagem", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                val photoFile = File(context.cacheDir, "pcb_${System.currentTimeMillis()}.jpg")
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    photoFile
                                )
                                tempCameraUri = uri
                                cameraLauncher.launch(uri)
                            },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TechPrimary)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Tirar Foto", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, TechBorder)
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Galeria", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // RF3: Limiar de Confiança (Confidence Threshold)
            TechnicalCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Limiar de Confiança",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(Modifier.width(8.dp))
                        StatusBadge(
                            text = "${(uiState.confidenceThreshold * 100).toInt()}%",
                            containerColor = TechPrimaryContainer,
                            contentColor = TechPrimary
                        )
                    }
                    Slider(
                        value = uiState.confidenceThreshold,
                        onValueChange = { viewModel.setConfidenceThreshold(it) },
                        valueRange = 0.10f..1.00f,
                        steps = 17,
                        colors = SliderDefaults.colors(
                            thumbColor = TechPrimary,
                            activeTrackColor = TechPrimary
                        )
                    )
                }
            }

            // Pré-visualização da Imagem
            TechnicalCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Pré-visualização da Imagem",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(Modifier.width(8.dp))
                        if (uiState.selectedImageUri != null) {
                            StatusBadge(text = "Pronta para Inferência", containerColor = Color(0xFFDCFCE7), contentColor = TechSuccess)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, TechBorder, RoundedCornerShape(8.dp))
                            .background(TechSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.selectedImageUri != null) {
                            Image(
                                painter = rememberAsyncImagePainter(uiState.selectedImageUri),
                                contentDescription = "Preview PCB",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    modifier = Modifier.size(44.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Nenhuma imagem selecionada",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "Capture uma foto ou selecione da galeria",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // RF4: Botão Principal de Execução de Inferência On-Device
            Button(
                onClick = {
                    viewModel.runInference()
                    onNavigateToResult()
                },
                enabled = uiState.selectedBitmap != null && !uiState.isRunningInference,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TechPrimary)
            ) {
                if (uiState.isRunningInference) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Processando Inferência Local...", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Executar Inferência On-Device", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    // Diálogo para Configuração do Endereço do Servidor Backend
    if (showServerDialog) {
        AlertDialog(
            onDismissRequest = { showServerDialog = false },
            title = { Text("Configurar Servidor Backend REST", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Defina o endereço IP e porta da API REST para sincronização automática:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = serverUrlInput,
                        onValueChange = { serverUrlInput = it },
                        label = { Text("URL Base da API") },
                        placeholder = { Text("http://10.10.10.113:8080/") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Botão de atalho rápido para o IP do seu PC Wi-Fi
                    OutlinedButton(
                        onClick = { serverUrlInput = "http://10.10.10.113:8080/" },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Usar IP Wi-Fi deste PC (10.10.10.113:8080)", fontSize = 11.sp)
                    }

                    Text(
                        "Dicas de Conexão:\n• Celular físico (Wi-Fi): http://10.10.10.113:8080/\n• Emulador Android Studio: http://10.0.2.2:8080/",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.setBackendUrl(serverUrlInput)
                    showServerDialog = false
                }) {
                    Text("Salvar e Conectar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showServerDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
