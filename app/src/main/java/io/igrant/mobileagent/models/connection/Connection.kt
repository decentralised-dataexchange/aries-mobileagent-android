package io.igrant.mobileagent.models.connection

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class Connection {

    @SerializedName("@type")
    @Expose
    var type: String? = ""

    @SerializedName("@id")
    @Expose
    var id: String? = ""

    @SerializedName("name")
    @Expose
    var name: String? = ""

    @SerializedName("policy_url")
    @Expose
    var policyUrl: String? = ""

    @SerializedName("org_type")
    @Expose
    var orgType: String? = ""

    @SerializedName("logo_image_url")
    @Expose
    var logoImageUrl: String? = ""

    @SerializedName("location")
    @Expose
    var location: String? = ""

    @SerializedName("privacy_dashboard")
    @Expose
    var privacyDashboard: PrivacyDashboard? = null

    @SerializedName("cover_image_url")
    @Expose
    var coverImageUrl: String? = ""

    @SerializedName("description")
    @Expose
    var description: String? = ""

    @SerializedName("eula_url")
    @Expose
    var eulaUrl: String? = ""
}