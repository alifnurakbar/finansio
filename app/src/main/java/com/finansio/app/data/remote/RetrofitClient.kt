package com.finansio.app.data.remote

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Membuat klien HTTP (OkHttp + Retrofit) untuk Groq: header Authorization, logging (API key disamarkan), dan batas waktu. */
class GroqAuthInterceptor(private val apiKey: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .build()
        return chain.proceed(request)
    }
}

object RetrofitClient {

    /**
     * [httpLogging] mencatat isi lengkap permintaan dan jawaban Groq ke logcat. Isinya bisa memuat
     * foto struk, teks struk, dan ringkasan keuangan pengguna, jadi hanya boleh aktif di build debug.
     */
    fun createGroqApiService(httpLogging: Boolean): GroqApiService {
        val builder = OkHttpClient.Builder()
            .addInterceptor(GroqAuthInterceptor(GroqApiConstants.API_KEY))
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
        if (httpLogging) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                    redactHeader("Authorization")
                }
            )
        }
        return Retrofit.Builder()
            .baseUrl(GroqApiConstants.BASE_URL)
            .client(builder.build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GroqApiService::class.java)
    }
}
