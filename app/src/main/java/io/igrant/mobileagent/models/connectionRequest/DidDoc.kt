package io.igrant.mobileagent.models.connectionRequest

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class DidDoc {
    @SerializedName("@context")
    @Expose
    var context: String? = null

    @SerializedName("id")
    @Expose
    var id: String? = null

    @SerializedName("publicKey")
    @Expose
    var publicKey: ArrayList<PublicKey>? = null

    @SerializedName("authentication")
    @Expose
    var authentication: ArrayList<Authentication>? = null

    @SerializedName("service")
    @Expose
    var service: ArrayList<Service>? = null
}