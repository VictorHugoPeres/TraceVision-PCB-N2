package com.example.trabalhodan2.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.trabalhodan2.data.model.BoundingBox
import com.example.trabalhodan2.data.model.ImageDimension
import com.example.trabalhodan2.data.model.InferenceResult
import com.example.trabalhodan2.data.network.RetrofitClient
import com.example.trabalhodan2.ml.YoloOnnxDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class UiState(
    val selectedImageUri: Uri? = null,
    val selectedBitmap: Bitmap? = null,
    val modelName: String = "best.onnx (Assets)",
    val isModelLoaded: Boolean = false,
    val confidenceThreshold: Float = 0.50f,
    val isRunningInference: Boolean = false,
    val currentInferenceResult: InferenceResult? = null,
    val historyList: List<InferenceResult> = emptyList(),
    val selectedHistoryItem: InferenceResult? = null,
    val backendUrl: String = RetrofitClient.DEFAULT_BASE_URL,
    val isBackendOnline: Boolean = false,
    val isCheckingBackend: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class InferenceViewModel(application: Application) : AndroidViewModel(application) {

    private val detector = YoloOnnxDetector(application.applicationContext)
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    init {
        // Carrega o modelo padrão dos assets de forma assíncrona
        viewModelScope.launch(Dispatchers.IO) {
            val success = detector.loadModelFromAssets("best.onnx")
            _uiState.update {
                it.copy(
                    modelName = if (success) "best.onnx (ONNX Ativo)" else "best.onnx (Modo Simulado)",
                    isModelLoaded = success
                )
            }
        }
        // Carrega histórico inicial prévio para demonstração
        loadInitialHistory()
        // Testa a conexão inicial com o backend
        checkBackendHealth()
    }

    private fun loadInitialHistory() {
        val mockHistory = listOf(
            InferenceResult(
                sessionId = "sessao_20260918_001",
                timestamp = "2026-09-18T23:06:00Z",
                modelName = "best.onnx",
                executionTimeMs = 909L,
                totalObjects = 3,
                averageConfidence = 0.91f,
                imageDimension = ImageDimension(1080, 1920),
                boundingBoxes = listOf(
                    BoundingBox(
                        boxId = 1,
                        classLabel = "broken_circuit",
                        confidence = 0.93f,
                        widthPx = 120.5f,
                        heightPx = 45.0f,
                        centroidX = 210.25f,
                        centroidY = 422.50f,
                        areaPx = 5422.5f,
                        xMin = 150f,
                        yMin = 400f
                    ),
                    BoundingBox(
                        boxId = 2,
                        classLabel = "burn_down",
                        confidence = 0.90f,
                        widthPx = 115.0f,
                        heightPx = 48.0f,
                        centroidX = 350.00f,
                        centroidY = 510.00f,
                        areaPx = 5520.0f,
                        xMin = 292.5f,
                        yMin = 486f
                    ),
                    BoundingBox(
                        boxId = 3,
                        classLabel = "missing_parts",
                        confidence = 0.89f,
                        widthPx = 180.0f,
                        heightPx = 180.0f,
                        centroidX = 650.00f,
                        centroidY = 800.00f,
                        areaPx = 32400.0f,
                        xMin = 560f,
                        yMin = 710f
                    )
                ),
                syncedWithBackend = true
            )
        )
        _uiState.update { it.copy(historyList = mockHistory) }
    }

    fun setConfidenceThreshold(threshold: Float) {
        _uiState.update { it.copy(confidenceThreshold = threshold) }
    }

    fun setBackendUrl(url: String) {
        _uiState.update { it.copy(backendUrl = url) }
        RetrofitClient.updateBaseUrl(url)
        checkBackendHealth()
    }

    fun checkBackendHealth() {
        _uiState.update { it.copy(isCheckingBackend = true) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.getInferenceHistory()
                val isOnline = response.isSuccessful
                _uiState.update {
                    it.copy(
                        isBackendOnline = isOnline,
                        isCheckingBackend = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isBackendOnline = false,
                        isCheckingBackend = false
                    )
                }
            }
        }
    }

    fun setSelectedImage(uri: Uri, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (bitmap != null) {
                    _uiState.update {
                        it.copy(
                            selectedImageUri = uri,
                            selectedBitmap = bitmap,
                            currentInferenceResult = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Erro ao carregar imagem: ${e.message}") }
            }
        }
    }

    fun loadModelFromUri(uri: Uri, fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = detector.loadModelFromUri(uri)
            if (success) {
                _uiState.update {
                    it.copy(
                        modelName = fileName,
                        isModelLoaded = true,
                        successMessage = "Modelo ONNX carregado com sucesso!"
                    )
                }
            } else {
                _uiState.update { it.copy(errorMessage = "Falha ao carregar modelo ONNX selecionado.") }
            }
        }
    }

    fun runInference() {
        val bitmap = _uiState.value.selectedBitmap ?: return
        _uiState.update { it.copy(isRunningInference = true, errorMessage = null) }

        viewModelScope.launch(Dispatchers.Default) {
            val (execTime, boxes) = detector.runInference(bitmap, _uiState.value.confidenceThreshold)

            val totalObjs = boxes.size
            val avgConf = if (totalObjs > 0) boxes.map { it.confidence }.average().toFloat() else 0f
            val sessionId = "sessao_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())

            val result = InferenceResult(
                sessionId = sessionId,
                timestamp = timestamp,
                modelName = _uiState.value.modelName.replace(" (ONNX Ativo)", "").replace(" (Modo Simulado)", ""),
                executionTimeMs = execTime,
                totalObjects = totalObjs,
                averageConfidence = avgConf,
                imageDimension = ImageDimension(bitmap.width, bitmap.height),
                boundingBoxes = boxes,
                syncedWithBackend = false
            )

            // RF5: Calcula centróides e métricas conforme a fórmula trigonométrica
            result.computeCentroids()

            // Atualiza o estado da UI e adiciona ao histórico cronológico (RF8)
            _uiState.update { state ->
                val updatedHistory = listOf(result) + state.historyList
                state.copy(
                    isRunningInference = false,
                    currentInferenceResult = result,
                    historyList = updatedHistory
                )
            }

            // RF6: Disparo automático em background do payload JSON para o backend
            sendResultToBackend(result)
        }
    }

    fun sendResultToBackend(result: InferenceResult) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.sendInferenceResult(result)
                if (response.isSuccessful) {
                    markSynced(result.sessionId)
                    _uiState.update { it.copy(isBackendOnline = true) }
                }
            } catch (e: Exception) {
                // Servidor backend pode estar offline no momento
                _uiState.update { it.copy(isBackendOnline = false) }
                e.printStackTrace()
            }
        }
    }

    fun fetchHistoryFromServer() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.apiService.getInferenceHistory()
                if (response.isSuccessful && response.body() != null) {
                    val remoteHistory = response.body()!!
                    _uiState.update { state ->
                        state.copy(
                            historyList = remoteHistory,
                            isBackendOnline = true,
                            successMessage = "Histórico sincronizado com o servidor com sucesso!"
                        )
                    }
                } else {
                    _uiState.update { it.copy(errorMessage = "Servidor respondeu com código ${response.code()}") }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isBackendOnline = false,
                        errorMessage = "Não foi possível conectar ao servidor backend (${e.localizedMessage ?: "Offline"})."
                    )
                }
            }
        }
    }

    private fun markSynced(sessionId: String) {
        _uiState.update { state ->
            val updatedHistory = state.historyList.map {
                if (it.sessionId == sessionId) it.copy(syncedWithBackend = true) else it
            }
            val updatedCurrent = if (state.currentInferenceResult?.sessionId == sessionId) {
                state.currentInferenceResult.copy(syncedWithBackend = true)
            } else {
                state.currentInferenceResult
            }
            state.copy(
                historyList = updatedHistory,
                currentInferenceResult = updatedCurrent
            )
        }
    }

    fun selectHistoryItem(item: InferenceResult) {
        _uiState.update { it.copy(selectedHistoryItem = item) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        detector.close()
    }
}
