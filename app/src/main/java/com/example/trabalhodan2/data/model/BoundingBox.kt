package com.example.trabalhodan2.data.model

import com.google.gson.annotations.SerializedName

/**
 * Representa uma caixa delimitadora (Bounding Box) detectada na placa PCB.
 * Em conformidade estrita com o Enunciado da N2 (Seção 4 e Diagrama UML da Figura 1).
 */
data class BoundingBox(
    @SerializedName("id_caixa") val boxId: Int,
    @SerializedName("rotulo_classe") val classLabel: String,
    @SerializedName("confianca") val confidence: Float,
    @SerializedName("largura_px") val widthPx: Float,
    @SerializedName("altura_px") val heightPx: Float,
    @SerializedName("centroide_x") var centroidX: Float = 0f,
    @SerializedName("centroide_y") var centroidY: Float = 0f,
    @SerializedName("area_px2") var areaPx: Float = 0f,

    // Coordenadas absolutas na imagem para desenho da Bounding Box no Canvas
    @Transient val xMin: Float = 0f,
    @Transient val yMin: Float = 0f
) {
    /**
     * Calcula as coordenadas do centróide e área da caixa delimitadora:
     * X_c = X_min + (W_px / 2)
     * Y_c = Y_min + (H_px / 2)
     * A_px = W_px * H_px
     * 
     * Nota: O compilador Kotlin gera automaticamente o método JVM getAreaPx(): Float 
     * correspondente ao atributo areaPx, satisfazendo a interface UML.
     */
    fun computeCentroid() {
        centroidX = xMin + (widthPx / 2f)
        centroidY = yMin + (heightPx / 2f)
        areaPx = widthPx * heightPx
    }
}
