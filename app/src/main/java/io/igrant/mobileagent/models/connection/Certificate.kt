package io.igrant.mobileagent.models.connection

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import io.igrant.mobileagent.models.walletSearch.Record

class Certificate {

    @SerializedName("schema_version")
    @Expose
    var schemaVersion: String? = ""

    @SerializedName("schema_name")
    @Expose
    var schemaName: String? = ""

    @SerializedName("epoch")
    @Expose
    var epoch: String? = ""

    @SerializedName("schema_id")
    @Expose
    var schemaId: String? = ""

    @SerializedName("cred_def_id")
    @Expose
    var credDefId: String? = ""

    @SerializedName("schema_issuer_did")
    @Expose
    var schemaIssuerDid: String? = ""

    @SerializedName("issuer_did")
    @Expose
    var issuerDid: String? = ""

    @SerializedName("record")
    @Expose
    var record: Record? = null
}