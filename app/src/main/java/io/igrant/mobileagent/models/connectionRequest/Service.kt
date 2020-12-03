package io.igrant.mobileagent.models.connectionRequest

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class Service {

    @SerializedName("type")
    @Expose
    var type: String? = null

    @SerializedName("id")
    @Expose
    var id: String? = null

    @SerializedName("priority")
    @Expose
    var priority: Int? = null

    @SerializedName("routingKeys")
    @Expose
    var routingKeys: ArrayList<String>? = null

    @SerializedName("recipientKeys")
    @Expose
    var recipientKeys: ArrayList<String>? = null

    @SerializedName("serviceEndpoint")
    @Expose
    var serviceEndpoint: String? = null

}