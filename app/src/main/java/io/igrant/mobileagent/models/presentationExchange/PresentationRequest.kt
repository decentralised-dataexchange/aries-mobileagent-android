package io.igrant.mobileagent.models.presentationExchange

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import io.igrant.mobileagent.models.credentialExchange.Score
import java.util.*

class PresentationRequest {

    @SerializedName("nonce")
    @Expose
    var nonce: String? = ""

    @SerializedName("name")
    @Expose
    var name: String? = ""

    @SerializedName("version")
    @Expose
    var version: String? = ""

    @SerializedName("requested_attributes")
    @Expose
    var requestedAttributes: Map<String, RequestedAttribute>?= null

    @SerializedName("requested_predicates")
    @Expose
    var requestedPredicates: Object? = null

}