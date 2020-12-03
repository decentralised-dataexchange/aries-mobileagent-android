package io.igrant.mobileagent.models.walletSearch

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class Record {
    @SerializedName("type")
    @Expose
    var type: String? = null

    @SerializedName("id")
    @Expose
    var id: String? = null

    @SerializedName("value")
    @Expose
    var value: String? = null

    @SerializedName("tags")
    @Expose
    var tags: Any? = null
}