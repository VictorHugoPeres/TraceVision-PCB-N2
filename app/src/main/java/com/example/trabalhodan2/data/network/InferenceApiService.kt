package com.example.trabalhodan2.data.network

import com.example.trabalhodan2.data.model.BoundingBox
import com.example.trabalhodan2.data.model.InferenceResult
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface InferenceApiService {

    @POST("api/inferencia")
    suspend fun sendInferenceResult(@Body result: InferenceResult): Response<Void>

    @GET("api/inferencia/historico")
    suspend fun getInferenceHistory(): Response<List<InferenceResult>>

    @GET("api/inferencia/{id}/caixas")
    suspend fun getBoxDetails(@Path("id") sessionId: String): Response<List<BoundingBox>>
}
