package com.suno.android.sunointerview

import ApiResponse
import kotlinx.serialization.json.Json
import okhttp3.MediaType
import okhttp3.OkHttpClient
import retrofit2.Converter
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// Singleton object for the Retrofit client
object ApiClient {
    private const val BASE_URL = "https://apitest.suno.com/api/"

    // Private property to hold the Retrofit instance
    private var retrofit: Retrofit? = null

    // Static accessor method to get the Retrofit instance
    fun getInstance(): Retrofit {
        if (retrofit == null) {
            retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(OkHttpClient.Builder().build())
                .addConverterFactory(generateConverterFactory())
                .build()
        }
        return retrofit!!
    }

    private fun generateConverterFactory(): Converter.Factory {
        val json = Json {
            explicitNulls = false
            ignoreUnknownKeys = true
            isLenient = true
        }

        val contentType = MediaType.get("application/json")
        return json.asConverterFactory(contentType)
    }

}

interface ApiService {
    @GET("songs")
    suspend fun getSongs(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 10
    ) : Response<ApiResponse>
}
