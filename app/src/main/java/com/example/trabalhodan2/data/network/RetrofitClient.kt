package com.example.trabalhodan2.data.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Cliente HTTP Retrofit para comunicação com a API REST do Servidor Backend.
 * Em conformidade com o Requisito RF6 e Figura 1 do Enunciado N2.
 */
object RetrofitClient {
    const val DEFAULT_BASE_URL = "http://10.0.2.2:8080/"

    private var currentUrl: String = DEFAULT_BASE_URL

    val baseUrl: String
        get() = currentUrl

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private var _apiService: InferenceApiService = createApiService(currentUrl)

    val apiService: InferenceApiService
        get() = _apiService

    private fun createApiService(url: String): InferenceApiService {
        val formattedUrl = if (url.endsWith("/")) url else "$url/"
        return Retrofit.Builder()
            .baseUrl(formattedUrl)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(InferenceApiService::class.java)
    }

    fun updateBaseUrl(newUrl: String) {
        val formattedUrl = if (newUrl.endsWith("/")) newUrl else "$newUrl/"
        currentUrl = formattedUrl
        _apiService = createApiService(formattedUrl)
    }
}
