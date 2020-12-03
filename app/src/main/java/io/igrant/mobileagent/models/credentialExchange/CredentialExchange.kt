package io.igrant.mobileagent.models.credentialExchange

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import io.igrant.mobileagent.models.certificateOffer.Base64Extracted

class CredentialExchange {

    @SerializedName("thread_id")
    @Expose
    var threadId: String? = ""

    @SerializedName("created_at")
    @Expose
    var createdAt: String? = ""

    @SerializedName("updated_at")
    @Expose
    var updatedAt: String? = ""

    @SerializedName("connection_id")
    @Expose
    var connectionId: String? = ""

    @SerializedName("credential_proposal_dict")
    @Expose
    var credentialProposalDict: CredentialProposalDict? = null

    @SerializedName("credential_offer_dict")
    @Expose
    var credentialOfferDict: String? = null

    @SerializedName("credential_offer")
    @Expose
    var credentialOffer: Base64Extracted? = null

    @SerializedName("credential_request")
    @Expose
    var credentialRequest: CredentialRequest? = null

    @SerializedName("credential_request_metadata")
    @Expose
    var credentialRequestMetadata: CredentialRequestMetadata? = null

    @SerializedName("error_msg")
    @Expose
    var errorMsg: String? = null

    @SerializedName("auto_offer")
    @Expose
    var autoOffer: Boolean? = false

    @SerializedName("auto_issue")
    @Expose
    var autoIssue: Boolean? = false

    @SerializedName("auto_remove")
    @Expose
    var autoRemove: Boolean? = true

    @SerializedName("raw_credential")
    @Expose
    var rawCredential: RawCredential? = null

    @SerializedName("credential")
    @Expose
    var credential: String? = null

    @SerializedName("parent_thread_id")
    @Expose
    var parentThreadId: String? = null

    @SerializedName("initiator")
    @Expose
    var initiator: String? = "external"

    @SerializedName("credential_definition_id")
    @Expose
    var credentialDefinitionId: String? = ""

    @SerializedName("schema_id")
    @Expose
    var schemaId: String? = ""

    @SerializedName("credential_id")
    @Expose
    var credentialId: String? = null

    @SerializedName("revoc_reg_id")
    @Expose
    var revocRegId: String? = null

    @SerializedName("revocation_id")
    @Expose
    var revocationId: String? = null

    @SerializedName("role")
    @Expose
    var role: String? = "holder"

    @SerializedName("state")
    @Expose
    var state: String? = ""

    @SerializedName("trace")
    @Expose
    var trace: Boolean? = false
}