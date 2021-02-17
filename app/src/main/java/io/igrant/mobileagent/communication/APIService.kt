package io.igrant.mobileagent.communication

import io.igrant.mobileagent.models.agentConfig.ConfigPostResponse
import io.igrant.mobileagent.models.agentConfig.ConfigResponse
import io.igrant.mobileagent.models.qr.QrDecode
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Url

interface APIService {

    @GET(".well-known/agent-configuration")
    fun getAgentConfig(): Call<ConfigResponse>

    @POST(".well-known/agent-configuration")
    fun postDetails(@Body bytes:RequestBody ): Call<ResponseBody>

    @POST(" ")
    fun pollMessages(@Body bytes:RequestBody): Call<ResponseBody>

    @POST(" ")
    fun cloudConnection(@Body bytes:RequestBody): Call<ResponseBody>

    @POST
    fun postData(@Url url:String, @Body bytes:RequestBody): Call<ConfigPostResponse>

    @POST
    fun postDataWithoutData(@Url url:String, @Body bytes:RequestBody): Call<ResponseBody>

    @GET
    fun getGenesis(@Url url:String): Call<ResponseBody>

    @POST
    fun extractUrl(@Url url:String): Call<QrDecode>
}
