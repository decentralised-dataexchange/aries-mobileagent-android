package io.igrant.mobileagent.models.connection

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class ConnectionCerListResponse {

    @SerializedName("@type")
    @Expose
    var type: String? = ""

    @SerializedName("@id")
    @Expose
    var id: String? = ""

    @SerializedName("data_certificate_types")
    @Expose
    var dataCertificateTypes: ArrayList<Certificate>? = ArrayList()

}