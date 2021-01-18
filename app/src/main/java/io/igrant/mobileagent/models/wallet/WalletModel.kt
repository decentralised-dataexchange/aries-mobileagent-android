package io.igrant.mobileagent.models.wallet

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import io.igrant.mobileagent.models.MediatorConnectionObject
import io.igrant.mobileagent.models.credentialExchange.CredentialProposalDict
import io.igrant.mobileagent.models.credentialExchange.RawCredential
import java.io.Serializable

class WalletModel:Serializable {

    @SerializedName("raw_credential")
    @Expose
    var rawCredential: RawCredential? = null

    @SerializedName("credential_id")
    @Expose
    var credentialId: String? = null

    @SerializedName("connection")
    @Expose
    var connection: MediatorConnectionObject? = null

    @SerializedName("credential_proposal_dict")
    @Expose
    var credentialProposalDict: CredentialProposalDict? = null

}