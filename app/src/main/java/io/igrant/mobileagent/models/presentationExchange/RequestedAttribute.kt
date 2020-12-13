package io.igrant.mobileagent.models.presentationExchange

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import org.json.JSONArray

class RequestedAttribute {
    @SerializedName("name")
    @Expose
    var name: String? = null

    @SerializedName("names")
    @Expose
    var names: ArrayList<String>? = null

    @SerializedName("restrictions")
    @Expose
    var restrictions: ArrayList<CredId>? = null
}