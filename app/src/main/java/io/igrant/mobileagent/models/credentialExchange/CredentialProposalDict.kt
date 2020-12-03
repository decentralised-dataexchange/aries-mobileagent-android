package io.igrant.mobileagent.models.credentialExchange

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import io.igrant.mobileagent.models.certificateOffer.CredentialPreview

class CredentialProposalDict {

    @SerializedName("@type")
    @Expose
    var type: String? = ""

    @SerializedName("@id")
    @Expose
    var id: String? = ""

    @SerializedName("comment")
    @Expose
    var comment: String? = ""

    @SerializedName("cred_def_id")
    @Expose
    var credDefId: String? = ""

    @SerializedName("schema_id")
    @Expose
    var schemaId: String? = ""

    @SerializedName("credential_proposal")
    @Expose
    var credentialProposal: CredentialPreview? = null
}