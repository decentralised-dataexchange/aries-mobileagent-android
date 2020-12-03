package io.igrant.mobileagent.models.credentialExchange

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class PCredentials {

    @SerializedName("m_2")
    @Expose
    var m2: String? = null

    @SerializedName("a")
    @Expose
    var a: String? = null

    @SerializedName("e")
    @Expose
    var e: String? = null

    @SerializedName("v")
    @Expose
    var v: String? = null
}