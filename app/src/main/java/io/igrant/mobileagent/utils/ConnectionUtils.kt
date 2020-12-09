package io.igrant.mobileagent.utils

import com.google.gson.Gson
import io.igrant.mobileagent.indy.WalletManager
import io.igrant.mobileagent.models.MediatorConnectionObject
import io.igrant.mobileagent.models.walletSearch.SearchResponse
import org.hyperledger.indy.sdk.non_secrets.WalletSearch

object ConnectionUtils {
    fun checkIfConnectionAvailable(invitationKey: String): Boolean {
        val gson = Gson()
        val search = WalletSearch.open(
            WalletManager.getWallet,
            WalletRecordType.CONNECTION,
            "{\n" +
                    "  \"invitation_key\":\"$invitationKey\"\n" +
                    "}",
            "{ \"retrieveRecords\": true, \"retrieveTotalCount\": true, \"retrieveType\": false, \"retrieveValue\": true, \"retrieveTags\": true }"
        ).get()

        val connection =
            WalletSearch.searchFetchNextRecords(WalletManager.getWallet, search, 100).get()

        WalletManager.closeSearchHandle(search)
        val result = gson.fromJson(connection, SearchResponse::class.java)
        return result.totalCount ?: 0 > 0
    }

    fun getConnection(senderVk: String): MediatorConnectionObject? {

        val connectionSearchResult = SearchUtils.searchWallet(
            WalletRecordType.DID_KEY,
            "{\"key\": \"$senderVk\"}"
        )

        val did = connectionSearchResult.records?.get(0)?.tags?.get("did")


        val connection =
            SearchUtils.searchWallet(WalletRecordType.CONNECTION, "{\"their_did\":\"$did\"}")

        return if (connection.totalCount ?: 0 > 0)
            WalletManager.getGson.fromJson(
                connection.records?.get(0)?.value,
                MediatorConnectionObject::class.java
            )
        else
            null
    }
}