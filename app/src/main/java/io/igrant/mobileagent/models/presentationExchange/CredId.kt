package io.igrant.mobileagent.models.presentationExchange

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

class CredId:Serializable {
    @SerializedName("cred_def_id")
    @Expose
    var credDefId: String? = null
}