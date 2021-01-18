package io.igrant.mobileagent.models.credentialExchange

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class CredentialRequest {

    @SerializedName("prover_did")
    @Expose
    var proverDid: String? = null

    @SerializedName("cred_def_id")
    @Expose
    var credDefId: String? = null

    @SerializedName("blinded_ms")
    @Expose
    var blindedMs: BlindedMs? = null

    @SerializedName("blinded_ms_correctness_proof")
    @Expose
    var blindedMsCorrectnessProof: BlindedMsCorrectnessProof? = null

    @SerializedName("nonce")
    @Expose
    var nonce: String? = null
}