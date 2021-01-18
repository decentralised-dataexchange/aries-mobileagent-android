package io.igrant.mobileagent.models.connectionRequest

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class ConnectionRequest {

    @SerializedName("@type")
    @Expose
    var type: String? = "null"

    @SerializedName("@id")
    @Expose
    var id: String? = null

    @SerializedName("label")
    @Expose
    var label: String? = null

    @SerializedName("connection")
    @Expose
    var connection: DID? = null

    @SerializedName("~transport")
    @Expose
    var transport: Transport? = null

}