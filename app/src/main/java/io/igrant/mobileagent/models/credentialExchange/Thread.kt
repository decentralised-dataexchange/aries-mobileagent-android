package io.igrant.mobileagent.models.credentialExchange

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

class Thread :Serializable{

    @SerializedName("thid")
    @Expose
    var thid: String? = ""

}