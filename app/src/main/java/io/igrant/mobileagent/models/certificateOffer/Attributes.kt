package io.igrant.mobileagent.models.certificateOffer

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

class Attributes :Serializable {

    @SerializedName("name")
    @Expose
    var name: String? = ""

    @SerializedName("value")
    @Expose
    var value: String? = ""
}