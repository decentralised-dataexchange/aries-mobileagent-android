package io.igrant.mobileagent.models.presentationExchange

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class RequestedAttribute {
    @SerializedName("name")
    @Expose
    var name: String? = ""

    @SerializedName("restrictions")
    @Expose
    var restrictions: ArrayList<String>? = ArrayList()
}