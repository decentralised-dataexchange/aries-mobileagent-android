package io.igrant.mobileagent.models.certificateOffer

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

class CredentialPreview :Serializable{

    @SerializedName("@type")
    @Expose
    var type: String? = ""

    @SerializedName("attributes")
    @Expose
    var attributes: ArrayList<Attributes>? = ArrayList()


}