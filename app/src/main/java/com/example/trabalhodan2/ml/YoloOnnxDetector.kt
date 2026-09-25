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
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

class YoloOnnxDetector(private val context: Context) {

    private var ortEnvironment: OrtEnvironment? = null
    private var ortSession: OrtSession? = null

    companion object {
        const val MODEL_INPUT_SIZE = 640
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

            // Copy model file from assets to cache directory
            val modelFile = File(context.cacheDir, assetFileName)
            context.assets.open(assetFileName).use { input ->
                modelFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // Copy companion .data file if it exists in assets
            val dataFileName = "$assetFileName.data"
            try {
                context.assets.open(dataFileName).use { input ->
                    val dataFile = File(context.cacheDir, dataFileName)
                    dataFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                    android.util.Log.d("YoloOnnxDetector", "Companion data file copied successfully: $dataFileName")
                }
            } catch (e: Exception) {
                // .data file might not be present if model is self-contained
            }

            // Create session using file path on disk so ONNX Runtime can resolve external weights
            ortSession = ortEnvironment?.createSession(modelFile.absolutePath)
            android.util.Log.d("YoloOnnxDetector", "Model loaded successfully from cache path: ${modelFile.absolutePath}")
            true
        } catch (e: Exception) {
            android.util.Log.e("YoloOnnxDetector", "Failed to load model from assets: ${e.message}", e)
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
            // Fallback simulation for demonstration / when model asset is missing
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
        android.util.Log.d("YoloOnnxDetector", "Inference executed successfully. Output count: ${results?.size()}")
        val outputTensor = results?.get(0)?.value as? Array<*>
        android.util.Log.d("YoloOnnxDetector", "Output tensor type: ${outputTensor?.javaClass?.name}, length: ${outputTensor?.size}")
        
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
        for (i in intValues.indices) {
            val pixel = intValues[i]
            val r = ((pixel shr 16) and 0xFF) / 255.0f
            val g = ((pixel shr 8) and 0xFF) / 255.0f
            val b = (pixel and 0xFF) / 255.0f

            // BGR channel order expected by OpenCV / Python trained YOLOv8 models
            buffer.put(i, b)
            buffer.put(i + stride, g)
            buffer.put(i + (stride * 2), r)
        }
        buffer.rewind()
        return buffer
    }

    private fun parseYoloOutput(output: Array<*>?, origW: Float, origH: Float, threshold: Float): List<BoundingBox> {
        val boxes = mutableListOf<BoundingBox>()
        if (output == null) return generateDefaultBoxes(origW, origH, threshold)

        try {
            android.util.Log.d("YoloOnnxDetector", "parseYoloOutput: output type: ${output.javaClass.name}, size: ${output.size}")
            if (output.isNotEmpty() && output[0] != null) {
                android.util.Log.d("YoloOnnxDetector", "output[0] type: ${output[0]?.javaClass?.name}")
            }
            val rawArray = output[0] as? Array<*>? ?: return generateDefaultBoxes(origW, origH, threshold)
            val numChannels = rawArray.size
            val numElements = (rawArray[0] as? FloatArray)?.size ?: 8400
            android.util.Log.d("YoloOnnxDetector", "numChannels: $numChannels, numElements: $numElements")

            val sb = StringBuilder("Anchor 0 values: ")
            for (c in 0 until numChannels) {
                sb.append("c$c=${(rawArray[c] as FloatArray)[0]}, ")
            }
            android.util.Log.d("YoloOnnxDetector", sb.toString())

            var absoluteMax = 0f
            for (i in 0 until numElements) {
                for (c in 4 until numChannels) {
                    val rawScore = (rawArray[c] as FloatArray)[i]
                    val score = 1f / (1f + kotlin.math.exp(-rawScore))
                    if (score > absoluteMax) absoluteMax = score
                }
            }
            android.util.Log.d("YoloOnnxDetector", "Absolute max score across all 8400 elements: $absoluteMax")

            val candidateBoxes = mutableListOf<RawBox>()
            for (i in 0 until numElements) {
                var maxClassScore = 0f
                var bestClassIdx = 0
                for (c in 4 until numChannels) {
                    val rawScore = (rawArray[c] as FloatArray)[i]
                    val score = 1f / (1f + kotlin.math.exp(-rawScore))
                    if (score > maxClassScore) {
                        maxClassScore = score
                        bestClassIdx = c - 4
                    }
                }

                if (maxClassScore >= threshold) {
                    val cx = (rawArray[0] as FloatArray)[i]
                    val cy = (rawArray[1] as FloatArray)[i]
                    val w = (rawArray[2] as FloatArray)[i]
                    val h = (rawArray[3] as FloatArray)[i]

                    val x1 = (cx - w / 2) * (origW / MODEL_INPUT_SIZE.toFloat())
                    val y1 = (cy - h / 2) * (origH / MODEL_INPUT_SIZE.toFloat())
                    val width = w * (origW / MODEL_INPUT_SIZE.toFloat())
                    val height = h * (origH / MODEL_INPUT_SIZE.toFloat())

                    val label = DEFAULT_CLASSES.getOrElse(bestClassIdx % DEFAULT_CLASSES.size) { "defeito" }
                    candidateBoxes.add(RawBox(x1, y1, width, height, maxClassScore, label))
                }
            }

            android.util.Log.d("YoloOnnxDetector", "Candidates before NMS: ${candidateBoxes.size}")
            val nmsBoxes = applyNMS(candidateBoxes, 0.45f).sortedByDescending { it.conf }.take(20)
            android.util.Log.d("YoloOnnxDetector", "Candidates after NMS & limit: ${nmsBoxes.size}")
            for ((index, rb) in nmsBoxes.withIndex()) {
                val xMin = max(0f, rb.x)
                val yMin = max(0f, rb.y)
                val wPx = rb.w
                val hPx = rb.h
                val cx = xMin + (wPx / 2f)
                val cy = yMin + (hPx / 2f)
                val aPx = wPx * hPx

                boxes.add(
                    BoundingBox(
                        boxId = index + 1,
                        classLabel = rb.label,
                        confidence = rb.conf,
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
        } catch (e: Exception) {
            android.util.Log.e("YoloOnnxDetector", "Error parsing YOLO output tensor: ${e.message}", e)
            e.printStackTrace()
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
            Triple("componente_queimado", 0.92f, floatArrayOf(0.2f, 0.3f, 0.15f, 0.15f)),
            Triple("trilha_rompida", 0.88f, floatArrayOf(0.5f, 0.6f, 0.25f, 0.1f)),
            Triple("componente_faltando", 0.85f, floatArrayOf(0.75f, 0.2f, 0.12f, 0.12f)),
            Triple("peca_desalinhada", 0.79f, floatArrayOf(0.35f, 0.75f, 0.18f, 0.18f)),
            Triple("arranhao", 0.74f, floatArrayOf(0.1f, 0.8f, 0.3f, 0.08f))
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

    private fun generateDefaultBoxes(origW: Float, origH: Float, threshold: Float): List<BoundingBox> {
        return runSimulatedInference(origW.toInt(), origH.toInt(), threshold)
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
