package io.igrant.mobileagent.models.certificateOffer

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

class OfferData :Serializable {

    @SerializedName("base64")
    @Expose
    var base64: String? = ""
}