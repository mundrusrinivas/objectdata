package com.example.objectai


data class Content(
    val parts: List<Part>
)

data class Part(
    val text: String? = null,
    val inline_data: InlineData? = null
)

data class InlineData(
    val mime_type: String,
    val data: String
)

data class GenerativeResponse(
    val text: String,  // The AI-generated text (description or result)
    val candidates: List<String>? // A list of candidate options or additional suggestions
)


