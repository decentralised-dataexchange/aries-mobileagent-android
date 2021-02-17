package io.igrant.mobileagent.models.agentConfig

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

class Invitation:Serializable {

    @SerializedName("label")
    @Expose
    var label: String? = null

    @SerializedName("imageUrl")
    @Expose
    var imageUrl: String? = null

    @SerializedName("image_url")
    @Expose
    var image_url: String? = null

    @SerializedName("serviceEndpoint")
    @Expose
    var serviceEndpoint: String? = null

    @SerializedName("routingKeys")
    @Expose
    var routingKeys: ArrayList<String>? = null

    @SerializedName("recipientKeys")
    @Expose
    var recipientKeys: ArrayList<String>? = null

    @SerializedName("@id")
    @Expose
    var id: String? = null

    @SerializedName("@type")
    @Expose
    var type: String? = null
}