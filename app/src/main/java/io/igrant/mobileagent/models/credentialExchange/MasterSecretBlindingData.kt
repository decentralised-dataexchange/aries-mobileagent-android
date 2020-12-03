package io.igrant.mobileagent.models.credentialExchange

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class MasterSecretBlindingData {

    @SerializedName("v_prime")
    @Expose
    var vPrime: String? = null

    @SerializedName("vr_prime")
    @Expose
    var vrPrime: String? = null
}