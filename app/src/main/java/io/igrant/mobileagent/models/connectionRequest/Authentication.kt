package io.igrant.mobileagent.models.connectionRequest

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class Authentication {

    @SerializedName("type")
    @Expose
    var type: String? = null

    @SerializedName("publicKey")
    @Expose
    var publicKey: String? = null

}