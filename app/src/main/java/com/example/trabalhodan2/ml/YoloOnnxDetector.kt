package com.example.trabalhodan2.ml

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.trabalhodan2.data.model.BoundingBox
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.File
import java.io.InputStream
import java.nio.FloatBuffer
import kotlin.math.max
import kotlin.math.min

class YoloOnnxDetector(private val context: Context) {

    private var ortEnvironment: OrtEnvironment? = null
    private var ortSession: OrtSession? = null

    companion object {
        const val MODEL_INPUT_SIZE = 640

        // As 7 classes oficiais de detecção de defeitos de PCB treinadas no best.onnx
        val DEFAULT_CLASSES = listOf(
            "broken_circuit",
            "burn_down",
            "damage",
            "less_tin",
            "missing_parts",
            "more_tin",
            "shifting"
        )
    }

    fun loadModelFromAssets(assetFileName: String = "best.onnx"): Boolean {
        return try {
            close()
            ortEnvironment = OrtEnvironment.getEnvironment()

            val modelFile = File(context.cacheDir, assetFileName)
            context.assets.open(assetFileName).use { input ->
                modelFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            val dataFileName = "$assetFileName.data"
            try {
                context.assets.open(dataFileName).use { input ->
                    val dataFile = File(context.cacheDir, dataFileName)
                    dataFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                    android.util.Log.d("YoloOnnxDetector", "Companion data file copiado com sucesso: $dataFileName")
                }
            } catch (e: Exception) {
                // Arquivo .data pode não ser necessário se o modelo for autocontido
            }

            ortSession = ortEnvironment?.createSession(modelFile.absolutePath)
            android.util.Log.d("YoloOnnxDetector", "Modelo ONNX carregado com sucesso de: ${modelFile.absolutePath}")
            true
        } catch (e: Exception) {
            android.util.Log.e("YoloOnnxDetector", "Falha ao carregar modelo ONNX: ${e.message}", e)
            false
        }
    }

    fun loadModelFromUri(uri: Uri): Boolean {
        return try {
            close()
            ortEnvironment = OrtEnvironment.getEnvironment()
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            if (bytes != null) {
                ortSession = ortEnvironment?.createSession(bytes)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun runInference(bitmap: Bitmap, confidenceThreshold: Float): Pair<Long, List<BoundingBox>> {
        val startTime = System.currentTimeMillis()
        val origWidth = bitmap.width
        val origHeight = bitmap.height

        val boxes = if (ortSession != null && ortEnvironment != null) {
            runRealOnnxInference(bitmap, confidenceThreshold)
        } else {
            runSimulatedInference(origWidth, origHeight, confidenceThreshold)
        }

        val executionTime = System.currentTimeMillis() - startTime
        return Pair(executionTime, boxes)
    }

    private fun runRealOnnxInference(bitmap: Bitmap, confidenceThreshold: Float): List<BoundingBox> {
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE, true)
        val floatBuffer = preprocessBitmapToNCHW(resizedBitmap)

        val inputName = ortSession?.inputNames?.iterator()?.next() ?: "images"
        val shape = longArrayOf(1, 3, MODEL_INPUT_SIZE.toLong(), MODEL_INPUT_SIZE.toLong())

        val tensor = OnnxTensor.createTensor(ortEnvironment, floatBuffer, shape)
        val inputs: Map<String, OnnxTensor> = mapOf(inputName to tensor)

        val results = ortSession?.run(inputs)
        val outputTensor = results?.get(0)?.value as? Array<*>

        tensor.close()
        results?.close()

        return parseYoloOutput(outputTensor, bitmap.width.toFloat(), bitmap.height.toFloat(), confidenceThreshold)
    }

    private fun preprocessBitmapToNCHW(bitmap: Bitmap): FloatBuffer {
        val buffer = FloatBuffer.allocate(1 * 3 * MODEL_INPUT_SIZE * MODEL_INPUT_SIZE)
        buffer.rewind()

        val intValues = IntArray(MODEL_INPUT_SIZE * MODEL_INPUT_SIZE)
        bitmap.getPixels(intValues, 0, MODEL_INPUT_SIZE, 0, 0, MODEL_INPUT_SIZE, MODEL_INPUT_SIZE)

        val stride = MODEL_INPUT_SIZE * MODEL_INPUT_SIZE
        // Ultralytics YOLOv8 treina e infere estritamente no padrão RGB normalizado (/255.0f)
        for (i in intValues.indices) {
            val pixel = intValues[i]
            val r = ((pixel shr 16) and 0xFF) / 255.0f
            val g = ((pixel shr 8) and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f

            buffer.put(i, r)
            buffer.put(i + stride, g)
            buffer.put(i + (stride * 2), b)
        }
        buffer.rewind()
        return buffer
    }

    private fun parseYoloOutput(output: Array<*>?, origW: Float, origH: Float, threshold: Float): List<BoundingBox> {
        val boxes = mutableListOf<BoundingBox>()
        if (output == null || output.isEmpty()) return emptyList()

        try {
            val rawArray = output[0] as? Array<*>? ?: return emptyList()
            val numChannels = rawArray.size
            val numElements = (rawArray[0] as? FloatArray)?.size ?: 8400

            val candidateBoxes = mutableListOf<RawBox>()
            val scaleX = origW / MODEL_INPUT_SIZE.toFloat()
            val scaleY = origH / MODEL_INPUT_SIZE.toFloat()

            for (i in 0 until numElements) {
                var maxClassScore = 0f
                var bestClassIdx = 0

                // Canais 4 a 10 contêm as probabilidades das 7 classes (já ativadas com Sigmoid pelo Ultralytics)
                for (c in 4 until numChannels) {
                    val score = (rawArray[c] as FloatArray)[i]
                    if (score > maxClassScore) {
                        maxClassScore = score
                        bestClassIdx = c - 4
                    }
                }

                // Apenas candidatos cuja confiança real supere o limiar configurado
                if (maxClassScore >= threshold) {
                    val cx = (rawArray[0] as FloatArray)[i]
                    val cy = (rawArray[1] as FloatArray)[i]
                    val w = (rawArray[2] as FloatArray)[i]
                    val h = (rawArray[3] as FloatArray)[i]

                    val x1 = (cx - w / 2f) * scaleX
                    val y1 = (cy - h / 2f) * scaleY
                    val boxW = w * scaleX
                    val boxH = h * scaleY

                    // Garante que a caixa permaneça dentro dos limites físicos da imagem
                    val clampedX = max(0f, min(x1, origW - 1f))
                    val clampedY = max(0f, min(y1, origH - 1f))
                    val clampedW = max(1f, min(boxW, origW - clampedX))
                    val clampedH = max(1f, min(boxH, origH - clampedY))

                    val label = DEFAULT_CLASSES.getOrElse(bestClassIdx % DEFAULT_CLASSES.size) { "defeito" }
                    candidateBoxes.add(RawBox(clampedX, clampedY, clampedW, clampedH, maxClassScore, label))
                }
            }

            android.util.Log.d("YoloOnnxDetector", "Candidatos detectados antes do NMS: ${candidateBoxes.size}")

            // Aplica Supressão de Não-Máximos (NMS) para remover caixas redundantes
            val nmsBoxes = applyNMS(candidateBoxes, 0.45f).take(30)
            android.util.Log.d("YoloOnnxDetector", "Caixas finais após NMS: ${nmsBoxes.size}")

            for ((index, rb) in nmsBoxes.withIndex()) {
                val cx = rb.x + (rb.w / 2f)
                val cy = rb.y + (rb.h / 2f)
                val aPx = rb.w * rb.h

                boxes.add(
                    BoundingBox(
                        boxId = index + 1,
                        classLabel = rb.label,
                        confidence = rb.conf,
                        xMin = rb.x,
                        yMin = rb.y,
                        widthPx = rb.w,
                        heightPx = rb.h,
                        centroidX = cx,
                        centroidY = cy,
                        areaPx = aPx
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("YoloOnnxDetector", "Erro ao processar tensor YOLO: ${e.message}", e)
            return emptyList()
        }

        return boxes
    }

    private data class RawBox(val x: Float, val y: Float, val w: Float, val h: Float, val conf: Float, val label: String)

    private fun applyNMS(boxes: List<RawBox>, iouThreshold: Float): List<RawBox> {
        val sorted = boxes.sortedByDescending { it.conf }
        val selected = mutableListOf<RawBox>()
        val active = BooleanArray(sorted.size) { true }

        for (i in sorted.indices) {
            if (!active[i]) continue
            val boxA = sorted[i]
            selected.add(boxA)

            for (j in i + 1 until sorted.size) {
                if (!active[j]) continue
                val boxB = sorted[j]
                if (calculateIoU(boxA, boxB) > iouThreshold) {
                    active[j] = false
                }
            }
        }
        return selected
    }

    private fun calculateIoU(a: RawBox, b: RawBox): Float {
        val x1 = max(a.x, b.x)
        val y1 = max(a.y, b.y)
        val x2 = min(a.x + a.w, b.x + b.w)
        val y2 = min(a.y + a.h, b.y + b.h)

        val interWidth = max(0f, x2 - x1)
        val interHeight = max(0f, y2 - y1)
        val interArea = interWidth * interHeight

        val unionArea = (a.w * a.h) + (b.w * b.h) - interArea
        return if (unionArea > 0f) interArea / unionArea else 0f
    }

    private fun runSimulatedInference(origW: Int, origH: Int, threshold: Float): List<BoundingBox> {
        val simulatedList = listOf(
            Triple("broken_circuit", 0.93f, floatArrayOf(0.15f, 0.22f, 0.18f, 0.08f)),
            Triple("burn_down", 0.89f, floatArrayOf(0.45f, 0.40f, 0.20f, 0.16f)),
            Triple("missing_parts", 0.85f, floatArrayOf(0.70f, 0.30f, 0.15f, 0.15f))
        )

        val boxes = mutableListOf<BoundingBox>()
        var idCounter = 1
        for ((label, conf, coords) in simulatedList) {
            if (conf >= threshold) {
                val xMin = coords[0] * origW
                val yMin = coords[1] * origH
                val wPx = coords[2] * origW
                val hPx = coords[3] * origH
                val cx = xMin + (wPx / 2f)
                val cy = yMin + (hPx / 2f)
                val aPx = wPx * hPx

                boxes.add(
                    BoundingBox(
                        boxId = idCounter++,
                        classLabel = label,
                        confidence = conf,
                        xMin = xMin,
                        yMin = yMin,
                        widthPx = wPx,
                        heightPx = hPx,
                        centroidX = cx,
                        centroidY = cy,
                        areaPx = aPx
                    )
                )
            }
        }
        return boxes
    }

    fun close() {
        try {
            ortSession?.close()
            ortEnvironment?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        ortSession = null
        ortEnvironment = null
    }
}
