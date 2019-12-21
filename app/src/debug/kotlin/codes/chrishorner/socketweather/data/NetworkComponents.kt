package codes.chrishorner.socketweather.data

import android.app.Application
import android.content.Context
import au.com.gridstone.debugdrawer.okhttplogs.HttpLogger
import au.com.gridstone.debugdrawer.retrofit.DebugRetrofitConfig
import au.com.gridstone.debugdrawer.retrofit.Endpoint
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.create
import retrofit2.mock.MockRetrofit
import retrofit2.mock.NetworkBehavior

class NetworkComponents private constructor(app: Application) {

  val api: WeatherApi
  val debugRetrofitConfig: DebugRetrofitConfig
  val httpLogger = HttpLogger(app, prettyPrintJson = true)

  init {
    val endpoints = listOf(
        Endpoint("Mock", "https://localhost/mock/", isMock = true),
        Endpoint("Production", DataConfig.API_ENDPOINT)
    )
    val networkBehavior = NetworkBehavior.create()
    debugRetrofitConfig = DebugRetrofitConfig(app, endpoints, networkBehavior)

    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(httpLogger.interceptor)
        .build()

    val currentEndpoint = debugRetrofitConfig.currentEndpoint
    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(currentEndpoint.url)
        .client(httpClient)
        .addConverterFactory(MoshiConverterFactory.create(DataConfig.moshi))
        .build()

    api = if (currentEndpoint.isMock) {
      MockRetrofit.Builder(retrofit)
          .networkBehavior(networkBehavior)
          .build()
          .let { MockWeatherApi(it) }
    } else {
      retrofit.create()
    }
  }

  companion object {
    private var instance: NetworkComponents? = null

    @Synchronized fun from(context: Context): NetworkComponents {
      instance?.let { return it }
      return NetworkComponents(context.applicationContext as Application).also { instance = it }
    }
  }
}