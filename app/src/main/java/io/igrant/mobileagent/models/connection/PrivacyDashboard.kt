package io.igrant.mobileagent.models.connection

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class PrivacyDashboard {

    @SerializedName("host_name")
    @Expose
    var hostName: String? = ""

    @SerializedName("version")
    @Expose
    var version: String? = ""

    @SerializedName("status")
    @Expose
    var status: Int? = null

    @SerializedName("delete")
    @Expose
    var delete: Boolean? = false
}