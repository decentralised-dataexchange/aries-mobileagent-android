package io.igrant.mobileagent.models

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.io.Serializable

class MediatorConnectionObject :Serializable{

    @SerializedName("inbox_id")
    @Expose
    var inboxId: String? = ""

    @SerializedName("inbox_key")
    @Expose
    var inboxKey: String? = ""

    @SerializedName("their_did")
    @Expose
    var theirDid: String? = ""

    @SerializedName("is_igrant_enabled")
    @Expose
    var isIGrantEnabled: Boolean? = false

    @SerializedName("request_id")
    @Expose
    var requestId: String? = ""

    @SerializedName("my_did")
    @Expose
    var myDid: String? = ""

    @SerializedName("invitation_key")
    @Expose
    var invitationKey: String? = ""

    @SerializedName("created_at")
    @Expose
    var createdAt: String? = null

    @SerializedName("updated_at")
    @Expose
    var updatedAt: String? = null

    @SerializedName("initiator")
    @Expose
    var initiator: String? = "external"

    @SerializedName("their_role")
    @Expose
    var theirRole: String? = null

    @SerializedName("inbound_connection_id")
    @Expose
    var inboundConnectionId: String? = null

    @SerializedName("routing_state")
    @Expose
    var routingState: String? = "none"

    @SerializedName("accept")
    @Expose
    var accept: String? = "manual"

    @SerializedName("invitation_mode")
    @Expose
    var invitationMode: String? = "once"

    @SerializedName("alias")
    @Expose
    var alias: String? = null

    @SerializedName("error_msg")
    @Expose
    var errorMsg: String? = null

    @SerializedName("their_label")
    @Expose
    var theirLabel: String? = null

    @SerializedName("their_image_url")
    @Expose
    var theirImageUrl: String? = null

    @SerializedName("state")
    @Expose
    var state: String? = null
}