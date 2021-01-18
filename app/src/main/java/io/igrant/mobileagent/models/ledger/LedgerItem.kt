package io.igrant.mobileagent.models.ledger

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class LedgerItem {

    @SerializedName("name")
    @Expose
    var name: String? = ""

    @SerializedName("type")
    @Expose
    var type: Int? = 0

}