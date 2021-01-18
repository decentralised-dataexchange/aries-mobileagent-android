package io.igrant.mobileagent.models.credentialExchange

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName
import java.util.*
import kotlin.collections.ArrayList

class BlindedMs {

    @SerializedName("u")
    @Expose
    var u: String? = null

    @SerializedName("ur")
    @Expose
    var ur: String? = null

    @SerializedName("hidden_attributes")
    @Expose
    var hiddenAttributes: ArrayList<String>? = ArrayList()

    @SerializedName("committed_attributes")
    @Expose
    var committedAttributes: Object? = null
}

class BlindedMsCorrectnessProof {

    @SerializedName("c")
    @Expose
    var c: String? = null

    @SerializedName("v_dash_cap")
    @Expose
    var vDashCap: String? = null

    @SerializedName("m_caps")
    @Expose
    var mCaps: MCaps? = null

    @SerializedName("r_caps")
    @Expose
    var rCaps: Object? = null

}

class MCaps {

    @SerializedName("master_secret")
    @Expose
    var masterSecret: String? = null
}