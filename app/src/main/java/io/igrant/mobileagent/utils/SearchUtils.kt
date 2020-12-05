package io.igrant.mobileagent.utils

import io.igrant.mobileagent.indy.WalletManager
import io.igrant.mobileagent.models.walletSearch.SearchResponse
import org.hyperledger.indy.sdk.non_secrets.WalletSearch

object SearchUtils {

    fun searchWallet(type: String,queryJson:String) :SearchResponse{
        val search = WalletSearch.open(
            WalletManager.getWallet,
            type,
            queryJson,
            "{ \"retrieveRecords\": true, \"retrieveTotalCount\": true, \"retrieveType\": false, \"retrieveValue\": true, \"retrieveTags\": true }"
        ).get()

        val searchResponse =
            WalletSearch.searchFetchNextRecords(
                WalletManager.getWallet,
                search,
                100
            ).get()

        WalletManager.closeSearchHandle(search)
       return WalletManager.getGson.fromJson(searchResponse, SearchResponse::class.java)
    }
}