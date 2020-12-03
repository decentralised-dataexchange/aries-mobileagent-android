package io.igrant.mobileagent.models.tagJsons

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class ConnectionTags {

    @SerializedName("invitation_key")
    @Expose
    var invitationKey: String? = ""

    @SerializedName("state")
    @Expose
    var state: String? = ""

    @SerializedName("their_did")
    @Expose
    var theirDid: String? = ""

    @SerializedName("request_id")
    @Expose
    var requestId: String? = ""

    @SerializedName("my_did")
    @Expose
    var myDid: String? = ""

}