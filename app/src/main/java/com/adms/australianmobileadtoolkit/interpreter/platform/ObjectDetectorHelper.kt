package com.adms.australianmobileadtoolkit.interpreter.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
import android.provider.Settings.Global
import android.util.Log
import com.adms.australianmobileadtoolkit.R
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.CompatibilityList
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.Rot90Op
import org.tensorflow.lite.support.metadata.MetadataExtractor
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext

class ObjectDetectorHelper(
    val context: Context,
    var threshold: Float = THRESHOLD_DEFAULT,
    var maxResults: Int = MAX_RESULTS_DEFAULT,
    var delegate: Delegate = Delegate.CPU,
    var model: Model = MODEL_DEFAULT,
) {
    private val imageProcessor = ImageProcessor.Builder()
        .add(NormalizeOp(INPUT_MEAN, INPUT_STANDARD_DEVIATION))
        .add(CastOp(INPUT_IMAGE_TYPE))
        .build()
    private var interpreter: Interpreter? = null
    private lateinit var labels: List<String>

    private val _detectionResult = MutableSharedFlow<DetectionResult>()
    val detectionResult: SharedFlow<DetectionResult> = _detectionResult

    private val _error = MutableSharedFlow<Throwable>()
    val error: SharedFlow<Throwable> = _error

    private var detectJob: Job? = null

    // Initialize the object detector using current settings on the
    // thread that is using it. CPU can be used with detectors
    // that are created on the main thread and used on a background thread, but
    // the GPU delegate needs to be used on the thread that initialized the detector
    suspend fun setupObjectDetector() {
        try {
            val litertBuffer = FileUtil.loadMappedFile(context, model.fileName)
            labels = getModelMetadata(litertBuffer)

            val compatList = CompatibilityList()
            val options = Interpreter.Options().apply{
                if(compatList.isDelegateSupportedOnThisDevice){
                    val delegateOptions = compatList.bestOptionsForThisDevice
                    this.addDelegate(GpuDelegate(delegateOptions))
                } else {
                    this.setNumThreads(4)
                }
            }


            interpreter = Interpreter(litertBuffer, options)
            Log.i(TAG, "Successfully init Interpreter!")
        } catch (e: Exception) {
            _error.emit(e)
            Log.e(TAG, "TFLite failed to load model with error: " + e.message)
        }
    }

    private fun processThisImage(bitmap: Bitmap) {
    }

    // Runs object detection on live streaming cameras frame-by-frame and returns the results
    // asynchronously to the caller.
    suspend fun detect(bitmap: Bitmap, rotationDegrees: Int) {
        if (interpreter == null) return
        val inputShape = interpreter!!.getInputTensor(0).shape()
        val outputShape = interpreter!!.getOutputTensor(0).shape()
        val tensorWidth = inputShape[1]
        val tensorHeight = inputShape[2]
        val numChannel = outputShape[1]
        val numElements = outputShape[2]







        val tensorImage = TensorImage(INPUT_IMAGE_TYPE)
        tensorImage.load(Bitmap.createScaledBitmap(BitmapFactory.decodeResource(context.getResources(), R.drawable.silver_tabby_cat_sitting_on_green_background_free_photo), tensorWidth, tensorHeight, false))
        val processedImage = imageProcessor.process(tensorImage)
        val imageBuffer = processedImage.buffer

        val output = TensorBuffer.createFixedSize(intArrayOf(1, numChannel, numElements), OUTPUT_IMAGE_TYPE)
        interpreter!!.run(imageBuffer, output.buffer)

        val bestBoxes = bestBox(output.floatArray, numElements, numChannel)












        /*
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, tensorWidth, tensorHeight, false)

        val tensorImage = TensorImage(INPUT_IMAGE_TYPE)
        tensorImage.load(resizedBitmap)
        val processedImage = imageProcessor.process(tensorImage)
        val imageBuffer = processedImage.buffer

        val output = TensorBuffer.createFixedSize(intArrayOf(1, numChannel, numElements), OUTPUT_IMAGE_TYPE)
        interpreter!!.run(imageBuffer, output.buffer)

        val bestBoxes = bestBox(output.floatArray,numElements, numChannel)*/










        /*
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, tensorWidth, tensorHeight, false)
        val tensorImageAlt = TensorImage(DataType.FLOAT32)
        tensorImageAlt.load(resizedBitmap)
        Log.i(TAG, "5")
        val imageProcessor = ImageProcessor.Builder()
            .add(NormalizeOp(INPUT_MEAN, INPUT_STANDARD_DEVIATION))
            .add(CastOp(INPUT_IMAGE_TYPE))
            .build() // preprocess input
        val processedImage = imageProcessor.process(tensorImageAlt)

        val imageBuffer = processedImage.buffer
        val outputAlt = TensorBuffer.createFixedSize(intArrayOf(1 , numChannel, numElements), OUTPUT_IMAGE_TYPE)
        interpreter!!.run(imageBuffer, outputAlt.buffer)
        val bestBoxes = bestBox(outputAlt.floatArray, numElements, numChannel)
        Log.i(TAG, bestBoxes.toString())*/
        Log.i(TAG, "5")
        withContext(Dispatchers.IO) {
            Log.i(TAG, "1")
            val startTime = SystemClock.uptimeMillis()
            Log.i(TAG, "2")
            val (_, h, w, _) = interpreter!!.getInputTensor(0).shape()
            Log.i(TAG, "3")

            // Preprocess the image and convert it into a TensorImage for classification.
            val tensorImage = createTensorImage(
                bitmap = bitmap, width = w, height = h, rotationDegrees = rotationDegrees
            )
            Log.i(TAG, "4")



            /*
            *

        val locationOutputShape = result.shape()
        Log.i(TAG, locationOutputShape[0].toString())
        Log.i(TAG, locationOutputShape[1].toString())
        Log.i(TAG, locationOutputShape[2].toString())
        val locationOutputBuffer =
            FloatBuffer.allocate(locationOutputShape[1] * locationOutputShape[2])
        //locationOutputBuffer.rewind()
        interpreter?.runForMultipleInputsOutputs(
            arrayOf(tensorImage.tensorBuffer.buffer), mapOf(
                Pair(0, locationOutputBuffer)
            )
        )
            *
            * */

            Log.i(TAG, "5")


            val output = detectImage(tensorImage, w, h)
            Log.i(TAG, "5")

            Log.i(TAG, output.toString())

            val locationOutput = output[0]
            val categoryOutput = output[1]
            val scoreOutput = output[2]
            val detections = getDetections(
                locations = locationOutput,
                categories = categoryOutput,
                scores = scoreOutput,
                width = w,
                scaleRatio = 1.0f//h.toFloat() / tensorImage.height
            )
            val inferenceTime = SystemClock.uptimeMillis() - startTime

            val detectionResult = DetectionResult(
                detections = detections,
                inferenceTime = inferenceTime,
                inputImageWidth = w,
                inputImageHeight = h,
            )
            _detectionResult.emit(detectionResult)
        }
    }

    private fun detectImage(tensorImage: TensorImage, w: Int, h: Int): List<FloatArray> {
        val outputShape = interpreter!!.getOutputTensor(0).shape()


        //val tensorWidth = w
        //val tensorHeight = h
        val numChannel = outputShape[1]
        val numElements = outputShape[2]
        val output = TensorBuffer.createFixedSize(intArrayOf(1 , numChannel, numElements), OUTPUT_IMAGE_TYPE)
        interpreter!!.run(tensorImage.buffer, output.buffer)
        //val e = output.floatArray
        Log.i(TAG, "end here")
        val bestBoxes = bestBox(output.floatArray, numElements, numChannel)

       // Log.i(TAG, result.shape()[0].toString())
        Log.i(TAG, "end here")
        val result = interpreter!!.getOutputTensor(0)
        val inputA = FloatBuffer.allocate(interpreter!!.getInputTensor(0).numElements());
        val outputA = FloatBuffer.allocate(result.numElements())
        Log.i(TAG, interpreter!!.run(inputA, outputA).toString())
        val xxx = arrayOf(outputA)
        Log.i(TAG, xxx.contentDeepToString())
        Log.i(TAG, "end here")

        val locationOutputShape = result.shape()
        Log.i(TAG, locationOutputShape[0].toString())
        Log.i(TAG, locationOutputShape[1].toString())
        Log.i(TAG, locationOutputShape[2].toString())
        val locationOutputBuffer =
            FloatBuffer.allocate(locationOutputShape[1] * locationOutputShape[2])
        //locationOutputBuffer.rewind()
        interpreter?.runForMultipleInputsOutputs(
            arrayOf(tensorImage.tensorBuffer.buffer), mapOf(
                Pair(0, locationOutputBuffer)
            )
        )
        Log.i(TAG, "aaa")

        val locationOutput = FloatArray(locationOutputBuffer.capacity())

        locationOutputBuffer.get(locationOutput)

        Log.i(TAG, locationOutputBuffer.toString())

        return listOf(locationOutput)
    }

    private fun createTensorImage(
        bitmap: Bitmap, width: Int, height: Int, rotationDegrees: Int
    ): TensorImage {
        val rotation = -rotationDegrees / 90

        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, false)
        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(resizedBitmap)


        val scaledBitmap = bitmap//fitCenterBitmap(bitmap, width, height)

        val imageProcessor = ImageProcessor.Builder()
            .add(NormalizeOp(INPUT_MEAN, INPUT_STANDARD_DEVIATION))
            .add(CastOp(INPUT_IMAGE_TYPE))
            .build() // preprocess input
        val processedImage = imageProcessor.process(tensorImage)

        // Preprocess the image and convert it into a TensorImage for classification.
        return processedImage
    }

    private fun fitCenterBitmap(
        originalBitmap: Bitmap,
        width: Int,
        height: Int
    ): Bitmap {
        val bitmapWithBackground = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmapWithBackground)
        canvas.drawColor(Color.TRANSPARENT)

        val scale: Float = height.toFloat() / originalBitmap.height
        val dstWidth = width * scale
        val processBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)

        val scaledBitmap = Bitmap.createScaledBitmap(
            processBitmap, dstWidth.toInt(), height, true
        )

        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        val left = (width - dstWidth) / 2
        canvas.drawBitmap(scaledBitmap, left, 0f, paint)
        return bitmapWithBackground
    }

    /** Load metadata from model*/
    private fun getModelMetadata(litertBuffer: ByteBuffer): List<String> {
        val metadataExtractor = MetadataExtractor(litertBuffer)
        val labels = mutableListOf<String>()
        if (metadataExtractor.hasMetadata()) {
            //val inputStream = metadataExtractor.getAssociatedFile("labelmap.txt")
            labels.addAll(listOf("Adidas", "Apple", "BMW", "Citroen", "Cocacola", "DHL", "Fedex", "Ferrari", "Ford", "Google", "Heineken", "HP", "Intel", "McDonalds", "Mini", "Nbc", "Nike", "Pepsi", "Porsche", "Puma", "RedBull", "Sprite", "Starbucks", "Texaco", "Unicef", "Vodafone", "Yahoo"))//readFileInputStream(inputStream))
            //readFileInputStream(inputStream))//
            Log.i(
                TAG, labels.toString()
            )
            Log.i(
                TAG, "Successfully loaded model metadata ${metadataExtractor.associatedFileNames}"
            )
        }
        return labels
    }

    /** Retrieve Map<String, Int> from metadata file */
    private fun readFileInputStream(inputStream: InputStream): List<String> {
        val reader = BufferedReader(InputStreamReader(inputStream))

        val list = mutableListOf<String>()
        var index = 0
        var line = ""
        while (reader.readLine().also { if (it != null) line = it } != null) {
            list.add(line)
            index++
        }

        reader.close()
        return list
    }

    private fun getDetections(
        locations: FloatArray,
        categories: FloatArray,
        scores: FloatArray,
        width: Int,
        scaleRatio: Float
    ): List<Detection> {
        val boundingBoxList = getBoundingBoxList(locations, width, scaleRatio)

        val detections = mutableListOf<Detection>()
        for (i in 0..<maxResults) {
            val categoryIndex = categories[i].toInt()
            detections.add(
                Detection(
                    label = labels[categoryIndex],
                    boundingBox = boundingBoxList[i],
                    score = scores[i]
                )
            )
        }

        return detections
            .filter { !it.boundingBox.isEmpty && it.score >= THRESHOLD_DEFAULT }
            .sortedByDescending { it.score }
    }

    /**
     * A tf.float32 tensor of shape [N, 4] containing bounding box coordinates
     * in the following order: [ymin, xmin, ymax, xmax]
     */
    private fun getBoundingBoxList(
        locations: FloatArray, width: Int, scaleRatio: Float
    ): Array<RectF> {
        val boundingBoxList = Array(locations.size / 4) { RectF() }
        val actualWidth = width * scaleRatio
        val padding = (width - width * scaleRatio) / 2

        for (i in boundingBoxList.indices) {
            val topRatio = locations[i * 4]
            val leftRatio = locations[i * 4 + 1]
            val bottomRatio = locations[i * 4 + 2]
            val rightRatio = locations[i * 4 + 3]

            val top = topRatio.coerceAtLeast(0f).coerceAtMost(1f)
            val left =
                ((leftRatio * width - padding) / actualWidth).coerceAtLeast(0f).coerceAtMost(1f)
            val bottom = bottomRatio.coerceAtLeast(top).coerceAtMost(1f)
            val right =
                ((rightRatio * width - padding) / actualWidth).coerceAtLeast(left).coerceAtMost(1f)

            val rectF = RectF(left, top, right, bottom)
            boundingBoxList[i] = rectF
        }

        return boundingBoxList;
    }

    fun detectImageObject( bitmap: Bitmap, rotationDegrees: Int) {
        val coroutineScope = CoroutineScope(context = EmptyCoroutineContext + CoroutineName(System.currentTimeMillis().toString()))
        detectJob = coroutineScope.launch {
            detect(bitmap, rotationDegrees)
        }
    }


    companion object {
        val MODEL_DEFAULT = Model.EfficientDetLite2
        const val MAX_RESULTS_DEFAULT = 5
        const val THRESHOLD_DEFAULT = 0.5F
        private const val INPUT_MEAN = 0f
        private const val INPUT_STANDARD_DEVIATION = 255f
        private val INPUT_IMAGE_TYPE = DataType.FLOAT32
        private val OUTPUT_IMAGE_TYPE = DataType.FLOAT32
        private const val CONFIDENCE_THRESHOLD = 0.3F
        private const val IOU_THRESHOLD = 0.5F

        const val TAG = "ObjectDetectorHelper"
    }

    enum class Model(val fileName: String) {
        //EfficientDetLite2("efficientdet_lite2.tflite"),//"best_int8.tflite"),
        EfficientDetLite2("yolov8s_float16.tflite"),//"best_int8.tflite"),
    }

    enum class Delegate(val value: Int) {
        CPU(0), NNAPI(1)
    }


    // Wraps results from inference, the time it takes for inference to be performed, and
    // the input image and height for properly scaling UI to return back to callers
    data class DetectionResult(
        val detections: List<Detection>,
        val inferenceTime: Long,
        val inputImageHeight: Int,
        val inputImageWidth: Int,
    )

    data class Detection(
        val label: String, val boundingBox: RectF, val score: Float
    )


    data class BoundingBox(
        val x1: Float,
        val y1: Float,
        val x2: Float,
        val y2: Float,
        val cx: Float,
        val cy: Float,
        val w: Float,
        val h: Float,
        val cnf: Float,
        val cls: Int,
        val clsName: String
    )

    private fun bestBox(array: FloatArray, numElements: Int, numChannel: Int) : List<BoundingBox>? {

        val boundingBoxes = mutableListOf<BoundingBox>()

        for (c in 0 until numElements) {
            var maxConf = CONFIDENCE_THRESHOLD
            var maxIdx = -1
            var j = 4
            var arrayIdx = c + numElements * j
            while (j < numChannel){
                if (array[arrayIdx] > maxConf) {
                    maxConf = array[arrayIdx]
                    maxIdx = j - 4
                }
                j++
                arrayIdx += numElements
            }

            if (maxConf > CONFIDENCE_THRESHOLD) {
                val clsName = labels[maxIdx]
                val cx = array[c] // 0
                val cy = array[c + numElements] // 1
                val w = array[c + numElements * 2]
                val h = array[c + numElements * 3]
                val x1 = cx - (w/2F)
                val y1 = cy - (h/2F)
                val x2 = cx + (w/2F)
                val y2 = cy + (h/2F)
                if (x1 < 0F || x1 > 1F) continue
                if (y1 < 0F || y1 > 1F) continue
                if (x2 < 0F || x2 > 1F) continue
                if (y2 < 0F || y2 > 1F) continue

                boundingBoxes.add(
                    BoundingBox(
                        x1 = x1, y1 = y1, x2 = x2, y2 = y2,
                        cx = cx, cy = cy, w = w, h = h,
                        cnf = maxConf, cls = maxIdx, clsName = clsName
                    )
                )
            }
        }

        if (boundingBoxes.isEmpty()) return null

        return applyNMS(boundingBoxes)
    }

    private fun applyNMS(boxes: List<BoundingBox>) : MutableList<BoundingBox> {
        val sortedBoxes = boxes.sortedByDescending { it.cnf }.toMutableList()
        val selectedBoxes = mutableListOf<BoundingBox>()

        while(sortedBoxes.isNotEmpty()) {
            val first = sortedBoxes.first()
            selectedBoxes.add(first)
            sortedBoxes.remove(first)

            val iterator = sortedBoxes.iterator()
            while (iterator.hasNext()) {
                val nextBox = iterator.next()
                val iou = calculateIoU(first, nextBox)
                if (iou >= IOU_THRESHOLD) {
                    iterator.remove()
                }
            }
        }

        return selectedBoxes
    }

    private fun calculateIoU(box1: BoundingBox, box2: BoundingBox): Float {
        val x1 = maxOf(box1.x1, box2.x1)
        val y1 = maxOf(box1.y1, box2.y1)
        val x2 = minOf(box1.x2, box2.x2)
        val y2 = minOf(box1.y2, box2.y2)
        val intersectionArea = maxOf(0F, x2 - x1) * maxOf(0F, y2 - y1)
        val box1Area = box1.w * box1.h
        val box2Area = box2.w * box2.h
        return intersectionArea / (box1Area + box2Area - intersectionArea)
    }


}