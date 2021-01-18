package io.igrant.mobileagent.models.connectionRequest

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class PublicKey {
    @SerializedName("type")
    @Expose
    var type: String? = null

    @SerializedName("id")
    @Expose
    var id: String? = null

    @SerializedName("controller")
    @Expose
    var controller: String? = null

    @SerializedName("publicKeyBase58")
    @Expose
    var publicKeyBase58: String? = null
}