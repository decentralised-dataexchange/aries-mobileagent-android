package io.igrant.mobileagent.models.credentialExchange

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class Signature {

    @SerializedName("p_credential")
    @Expose
    var pCredential: PCredentials? = null

    @SerializedName("r_credential")
    @Expose
    var rCredential: String? = null
}