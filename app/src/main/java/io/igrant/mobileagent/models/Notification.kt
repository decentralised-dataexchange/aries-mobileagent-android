package io.igrant.mobileagent.models

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import io.igrant.mobileagent.models.certificateOffer.CertificateOffer
import io.igrant.mobileagent.models.presentationExchange.PresentationExchange

class Notification {

    @SerializedName("type")
    @Expose
    var type: String? = null

    @SerializedName("date")
    @Expose
    var date: String? = null

    @SerializedName("connection")
    @Expose
    var connection: MediatorConnectionObject? = null

    @SerializedName("presentation")
    @Expose
    var presentation: PresentationExchange? = null

    @SerializedName("certificateOffer")
    @Expose
    var certificateOffer: CertificateOffer? = null
}