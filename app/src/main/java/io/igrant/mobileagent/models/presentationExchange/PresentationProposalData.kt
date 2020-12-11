package io.igrant.mobileagent.models.presentationExchange

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class PresentationProposalData {

    @SerializedName("@type")
    @Expose
    var type: String? = ""

    @SerializedName("@id")
    @Expose
    var id: String? = ""

    @SerializedName("presentation_proposal")
    @Expose
    var presentationProposal: PresentationProposal? = null

    @SerializedName("comment")
    @Expose
    var comment: String? = ""
}