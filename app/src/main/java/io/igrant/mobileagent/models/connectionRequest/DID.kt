package io.igrant.mobileagent.models.connectionRequest

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class DID {

    @SerializedName("DID")
    @Expose
    var did: String? = null

    @SerializedName("DIDDoc")
    @Expose
    var didDoc: DidDoc? = null

}