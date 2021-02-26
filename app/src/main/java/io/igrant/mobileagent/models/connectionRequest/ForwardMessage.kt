package io.igrant.mobileagent.models.connectionRequest

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import io.igrant.mobileagent.models.agentConfig.ConfigPostResponse

class ForwardMessage {

    @SerializedName("@type")
    @Expose
    var type: String? = null

    @SerializedName("@id")
    @Expose
    var id: String? = null

    @SerializedName("to")
    @Expose
    var to: String? = null

    @SerializedName("msg")
    @Expose
    var msg: ConfigPostResponse? = null
}