package io.igrant.mobileagent.utils

import androidx.fragment.app.FragmentManager
import com.google.gson.Gson
import io.igrant.mobileagent.fragment.ConnectionListFragment
import io.igrant.mobileagent.indy.WalletManager
import io.igrant.mobileagent.models.walletSearch.SearchResponse
import org.hyperledger.indy.sdk.non_secrets.WalletSearch

object ConnectionUtils {
    fun checkIfConnectionAvailable(invitationKey: String):Boolean {
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
}