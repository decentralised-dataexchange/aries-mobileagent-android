package io.igrant.mobileagent.models.tagJsons

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class InboxMessageTag {

    @SerializedName("connectionId")
    @Expose
    var connectionId: String? = null

    @SerializedName("type")
    @Expose
    var type: String? = null
}