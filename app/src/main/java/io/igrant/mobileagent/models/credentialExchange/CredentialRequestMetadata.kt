package io.igrant.mobileagent.models.credentialExchange

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class CredentialRequestMetadata {
    @SerializedName("master_secret_blinding_data")
    @Expose
    var masterSecretBlindingData: MasterSecretBlindingData? = null

    @SerializedName("nonce")
    @Expose
    var nonce: String? = null

    @SerializedName("master_secret_name")
    @Expose
    var masterSecretName: String? = null
}