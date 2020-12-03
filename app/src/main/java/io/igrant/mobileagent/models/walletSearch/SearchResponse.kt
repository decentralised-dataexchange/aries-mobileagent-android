package io.igrant.mobileagent.models.walletSearch

import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

class SearchResponse {

    @SerializedName("totalCount")
    @Expose
    var totalCount: Int? = 0

    @SerializedName("records")
    @Expose
    var records: ArrayList<Record>? = ArrayList()
}