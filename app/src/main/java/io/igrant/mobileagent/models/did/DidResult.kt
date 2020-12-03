package io.igrant.mobileagent.models.did

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class DidResult {

    @SerializedName("type")
    @Expose
    var type: String? = ""

    @SerializedName("id")
    @Expose
    var id: String? = ""

    @SerializedName("value")
    @Expose
    var value: String? = ""

    @SerializedName("tags")
    @Expose
    var tags: DidTag? = null
}