package com.example.objectai


data class RoboflowResponse(
    val inference_id: String,
    val time: Double,
    val image: Image,
    val predictions: List<Prediction>
)

data class Image(
    val width: Int,
    val height: Int
)

data class Prediction(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
    val confidence: Double,
    val `class`: String, // Escape the reserved keyword
    val class_id: Int,
    val detection_id: String
)
