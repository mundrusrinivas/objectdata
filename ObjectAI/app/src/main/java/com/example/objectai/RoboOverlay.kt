package com.example.objectai

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class RoboOverlay(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var predictions: List<Prediction> = listOf()
    private var scaleFactorX: Float = 1.0f
    private var scaleFactorY: Float = 1.0f

    // Paint for bounding box
    private val boxPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.RED
        strokeWidth = 4f
    }

    // Paint for label background
    private val textBackgroundPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.RED
        alpha = 160 // Semi-transparent background for better readability
    }

    // Paint for label text
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 40f
        typeface = Typeface.DEFAULT_BOLD
    }

    fun setPredictions(predictions: List<Prediction>, scaleFactorX: Float, scaleFactorY: Float) {
        this.predictions = predictions
        this.scaleFactorX = scaleFactorX
        this.scaleFactorY = scaleFactorY
        invalidate() // Request to redraw the view
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas?.let { canvas ->
            for (prediction in predictions) {
                // Calculate bounding box coordinates
                val left = (prediction.x - prediction.width / 2) * scaleFactorX
                val top = (prediction.y - prediction.height / 2) * scaleFactorY
                val right = (prediction.x + prediction.width / 2) * scaleFactorX
                val bottom = (prediction.y + prediction.height / 2) * scaleFactorY


                // Draw bounding box
                val rect = RectF(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
                canvas.drawRect(rect, boxPaint)

                // Prepare label text
                val label = "${prediction.`class`} (${String.format("%.2f", prediction.confidence)})"

                // Measure text dimensions
                val textWidth = textPaint.measureText(label)
                val textHeight = textPaint.textSize

                // Define label background rectangle
                var labelLeft = left
                var labelTop = top
                var labelRight = left + textWidth + 8 // Add horizontal padding
                var labelBottom = top + textHeight + 8 // Add vertical padding

                // Adjust if label background goes outside the view bounds
                if (labelRight > canvas.width) {
                    labelRight = canvas.width.toFloat().toDouble()
                    labelLeft = labelRight - textWidth - 8
                }
                if (labelBottom > height) {
                    labelBottom = canvas.height.toFloat().toDouble()
                    labelTop = labelBottom - textHeight - 8
                }

                val labelBackgroundRect = RectF(labelLeft.toFloat(),
                    labelTop.toFloat(), labelRight.toFloat(), labelBottom.toFloat()
                )

                // Draw label background
                canvas.drawRect(labelBackgroundRect, textBackgroundPaint)

                // Draw label text
                canvas.drawText(
                    label,
                    (labelLeft + 4).toFloat(), // Left padding
                    (labelTop + textHeight).toFloat(), // Align text baseline
                    textPaint
                )
            }
        }
    }
}