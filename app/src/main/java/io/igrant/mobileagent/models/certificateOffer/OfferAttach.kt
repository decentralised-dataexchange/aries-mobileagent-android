package io.igrant.mobileagent.models.certificateOffer

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

class OfferAttach :Serializable{

    @SerializedName("@id")
    @Expose
    var id: String? = ""

    @SerializedName("mime-type")
    @Expose
    var mimeType: String? = ""

    @SerializedName("data")
    @Expose
    var data: OfferData? = null

}