package io.igrant.mobileagent.communication

import android.util.Log
import com.google.gson.GsonBuilder
import okhttp3.*
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.lang.reflect.Proxy
import java.util.concurrent.TimeUnit

object ApiManager {
    fun getService(): APIService? {
        return service
    }

    private const val API_URL = "https://mediator.igrant.io/"

    private var okClient: OkHttpClient? = null
    private var service: APIService? = null
    private var httpClient: OkHttpClient.Builder? = null

    private var apiManager: ApiManager? = null
    val api: ApiManager
        get() {
            if (apiManager == null) {
                apiManager = ApiManager
                httpClient = OkHttpClient.Builder()
                val httpLoggingInterceptor = HttpLoggingInterceptor()
                httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
                httpClient!!.addInterceptor(httpLoggingInterceptor)
                httpClient!!.interceptors().add(HttpInterceptor())
                okClient = httpClient!!.readTimeout(120, TimeUnit.SECONDS)
                    .connectTimeout(120, TimeUnit.SECONDS).build()
                val gson = GsonBuilder()
                    .setLenient()
                    .create()
                val retrofit = Retrofit.Builder()
                    .baseUrl(API_URL)
                    .client(okClient!!)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build()
                service = retrofit.create(APIService::class.java)
            }
            return apiManager!!
        }

    fun resetApi() {
        apiManager = null
    }

    private class HttpInterceptor : Interceptor {

        @Throws(IOException::class)
        override fun intercept(chain: Interceptor.Chain): Response {
            var request = chain.request()
            val builder = request.newBuilder()
            setAuthHeader(builder)

            request = builder.build() //overwrite old request
            var response =
                chain.proceed(request) //perform request, here original request will be executed

            if (response.code == 401) { //if unauthorized
                Log.d("Unauth Hit", response.body.toString())
            }

            return response
        }

        //uncomment if there is token refresh
        private fun setAuthHeader(builder: Request.Builder) {
            builder.header("Content-Type", "application/ssi-agent-wire")
        }

    }
}
