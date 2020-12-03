package io.igrant.mobileagent.models.certificateOffer

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class Base64Extracted {

    @SerializedName("schema_id")
    @Expose
    var schemaId: String? = ""

    @SerializedName("cred_def_id")
    @Expose
    var credDefId: String? = ""

    @SerializedName("key_correctness_proof")
    @Expose
    var keyCorrectnessProof: KeyCorrectionProof? = null

    @SerializedName("nonce")
    @Expose
    var nonce: String? = ""
}