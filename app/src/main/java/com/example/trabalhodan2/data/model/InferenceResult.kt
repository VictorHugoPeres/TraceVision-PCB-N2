package com.example.trabalhodan2.data.model

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName

/**
 * Dimensão espacial da imagem analisada.
 */
data class ImageDimension(
    @SerializedName("largura_px") val widthPx: Int,
    @SerializedName("altura_px") val heightPx: Int
)

/**
 * Resultado completo da sessão de inferência de Visão Computacional.
 * Em conformidade estrita com o Enunciado da N2 (Seção 4 e Diagrama UML da Figura 1).
 */
data class InferenceResult(
    @SerializedName("id_sessao") val sessionId: String,
    @SerializedName("data_hora") val timestamp: String,
    @SerializedName("nome_modelo") val modelName: String,
    @SerializedName("tempo_execucao_ms") val executionTimeMs: Long,
    @SerializedName("total_objetos") val totalObjects: Int,
    @SerializedName("confianca_media") val averageConfidence: Float,
    @SerializedName("dimensao_imagem") val imageDimension: ImageDimension,
    @SerializedName("caixas_delimitadoras") val boundingBoxes: List<BoundingBox>,
    @Transient var syncedWithBackend: Boolean = false
) {
    /**
     * Calcula e atualiza os centróides e áreas de todas as caixas delimitadoras.
     */
    fun computeCentroids() {
        boundingBoxes.forEach { it.computeCentroid() }
    }

    /**
     * Converte o objeto para o payload JSON estipulado pelo enunciado da N2.
     */
    fun toJsonPayload(): String {
        return Gson().toJson(this)
    }

    // Atalhos para compatibilidade com a UI
    val imageWidth: Int get() = imageDimension.widthPx
    val imageHeight: Int get() = imageDimension.heightPx
}
