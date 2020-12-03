package io.igrant.mobileagent.models.agentConfig

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class ConfigPostResponse {

    @SerializedName("protected")
    @Expose
    var protected: String? = null

    @SerializedName("iv")
    @Expose
    var iv: String? = null

    @SerializedName("ciphertext")
    @Expose
    var ciphertext: String? = null

    @SerializedName("tag")
    @Expose
    var tag: String? = null
}