package io.igrant.mobileagent.models.qr

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class QrDecode {
    @SerializedName("invitation_url")
    @Expose
    var invitationUrl: String? = ""

    @SerializedName("dataexchange_url")
    @Expose
    var dataExchangeUrl: String? = ""
}