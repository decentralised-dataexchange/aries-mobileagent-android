package io.igrant.mobileagent.models.agentConfig

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class ConfigResponse {

    @SerializedName("ServiceEndpoint")
    @Expose
    var serviceEndpoint: String? = null

    @SerializedName("RoutingKey")
    @Expose
    var routingKey: String? = null

    @SerializedName("Invitation")
    @Expose
    var invitation: Invitation? = null

}