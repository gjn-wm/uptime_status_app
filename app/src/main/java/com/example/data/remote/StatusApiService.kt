package com.example.data.remote

import com.example.data.model.MonitorDetailDto
import com.example.data.model.StatusOverviewDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface StatusApiService {

  @GET("api/status")
  suspend fun getStatusOverview(): StatusOverviewDto

  @GET("api/monitors/{id}")
  suspend fun getMonitorDetail(
    @Path("id") monitorId: Int,
    @Query("range") range: String = "24h",
  ): MonitorDetailDto

  companion object {
    const val BASE_URL = "https://status.gjnwm.dpdns.org/"
    const val RSS_URL = "https://status.gjnwm.dpdns.org/feed.rss"

    fun create(): StatusApiService {
      val headerInterceptor = Interceptor { chain ->
        val request =
          chain
            .request()
            .newBuilder()
            .header("User-Agent", "GjnWMStatusAndroid/1.0")
            .header("Accept", "application/json")
            .header("Cache-Control", "no-cache")
            .build()
        chain.proceed(request)
      }

      val client =
        OkHttpClient.Builder()
          .addInterceptor(headerInterceptor)
          .connectTimeout(12, TimeUnit.SECONDS)
          .readTimeout(15, TimeUnit.SECONDS)
          .build()

      val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

      return Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(StatusApiService::class.java)
    }
  }
}
