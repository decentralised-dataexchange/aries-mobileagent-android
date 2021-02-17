package io.igrant.mobileagent.models.certificate

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class Certificate {
    @SerializedName("referent")
    @Expose
    var referent: String? = null

    @SerializedName("attrs")
    @Expose
    var attrs: Map<String,String>? = null

    @SerializedName("schema_id")
    @Expose
    var schemaId: String? = null

    @SerializedName("cred_def_id")
    @Expose
    var credDefId: String? = null

    @SerializedName("rev_reg_id")
    @Expose
    var revRegId: String? = null

    @SerializedName("cred_rev_id")
    @Expose
    var credRevId: String? = null
}