package io.igrant.mobileagent.models.did

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class DidTag {

    @SerializedName("key")
    @Expose
    var key: String? = ""

    @SerializedName("did")
    @Expose
    var did: String? = ""
}