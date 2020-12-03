package io.igrant.mobileagent.models.certificateOffer

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class KeyCorrectionProof {

    @SerializedName("c")
    @Expose
    var c: String? = ""

    @SerializedName("xz_cap")
    @Expose
    var xzCap: String? = ""

    @SerializedName("xr_cap")
    @Expose
    var xrCap: ArrayList<ArrayList<String>>? = ArrayList()


}