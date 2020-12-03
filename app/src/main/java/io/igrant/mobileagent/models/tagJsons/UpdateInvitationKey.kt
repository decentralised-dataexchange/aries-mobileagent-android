package io.igrant.mobileagent.models.tagJsons

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class UpdateInvitationKey(requestId:String?,myDid:String?,recipientKeys: String?,theirDid:String?) {

    @SerializedName("their_did")
    @Expose
    var theirDid: String? = theirDid

    @SerializedName("request_id")
    @Expose
    var requestId: String? = requestId

    @SerializedName("my_did")
    @Expose
    var myDid: String? = myDid

    @SerializedName("invitation_key")
    @Expose
    var invitationKey: String? = recipientKeys

}