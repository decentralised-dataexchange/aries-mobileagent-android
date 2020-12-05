package io.igrant.mobileagent.models.credentialExchange

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import org.json.JSONObject

class RawCredential {

    @SerializedName("schema_id")
    @Expose
    var schemaId: String? = null

    @SerializedName("cred_def_id")
    @Expose
    var credDefId: String? = null

    @SerializedName("rev_reg_id")
    @Expose
    var revRegId: String? = null

    @SerializedName("values")
    @Expose
    var values: Map<String, Score>? = null

    @SerializedName("signature")
    @Expose
    var signature: Signature? = null

    @SerializedName("signature_correctness_proof")
    @Expose
    var signatureCorrectnessProof: SignatureCorrectnessProof? = null

    @SerializedName("rev_reg")
    @Expose
    var revReg: String? = null

    @SerializedName("witness")
    @Expose
    var witness: String? = null
}