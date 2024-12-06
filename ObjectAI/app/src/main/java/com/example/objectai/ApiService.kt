package com.example.objectai

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    //v1/models/gemini-pro-vision:generateContent
   // @POST("v1beta/models/gemini-1.5-flash:generateContent")
    @POST("v1/models/gemini-pro-vision:generateContent")
    suspend fun detectSuspiciousObjects(
        @Query("key") apiKey: String,
        @Body contents: Content
    ): Response<String>

//https://detect.roboflow.com/weapon-detection-aoxpz/
    @Multipart
    @POST("{model}/{version}")
    fun detectObjects(
        @Path("model") model: String,
        @Path("version") version: String,
        @Query("api_key") apiKey: String,
        @Part image: MultipartBody.Part
    ): Call<RoboflowResponse>
}


