package io.igrant.mobileagent.models.credentialExchange

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class SignatureCorrectnessProof {

    @SerializedName("se")
    @Expose
    var se: String? = null

    @SerializedName("c")
    @Expose
    var c: String? = null
}