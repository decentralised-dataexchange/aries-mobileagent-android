package io.igrant.mobileagent.models.credentialExchange

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class Score {

    @SerializedName("raw")
    @Expose
    var raw: String? = null

    @SerializedName("encoded")
    @Expose
    var encoded: String? = null

}