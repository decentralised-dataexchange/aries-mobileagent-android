package io.igrant.mobileagent.models.tagJsons

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class InvitationKey(requestId:String?,myDid:String?,recipientKeys: String?) {

    @SerializedName("invitation_key")
    @Expose
    var invitationKey: String? = recipientKeys

}