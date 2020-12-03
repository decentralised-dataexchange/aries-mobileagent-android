package io.igrant.mobileagent.models.certificateOffer

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import io.igrant.mobileagent.models.credentialExchange.Thread
import java.io.Serializable

class CertificateOffer :Serializable{

    @SerializedName("@type")
    @Expose
    var type: String? = ""

    @SerializedName("@id")
    @Expose
    var id: String? = ""

    @SerializedName("~thread")
    @Expose
    var thread: Thread? = null

    @SerializedName("comment")
    @Expose
    var comment: String? = null

    @SerializedName("offers~attach")
    @Expose
    var offersAttach: ArrayList<OfferAttach>? = ArrayList()

    @SerializedName("credential_preview")
    @Expose
    var credentialPreview: CredentialPreview? = null
}