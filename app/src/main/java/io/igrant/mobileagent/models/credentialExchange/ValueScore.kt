package io.igrant.mobileagent.models.credentialExchange

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class ValueScore {

    @SerializedName("score")
    @Expose
    var score: Score? = Score()

}