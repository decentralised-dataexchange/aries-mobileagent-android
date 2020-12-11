package io.igrant.mobileagent.models.presentationExchange

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class PresentationProposal {

    @SerializedName("@type")
    @Expose
    var type: String? = ""

    @SerializedName("attributes")
    @Expose
    var attributes: ArrayList<ExchangeAttributes>? = ArrayList()

    @SerializedName("predicates")
    @Expose
    var predicates: ArrayList<String> = ArrayList()
}