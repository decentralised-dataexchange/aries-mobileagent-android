package io.igrant.mobileagent.utils

import android.util.Log
import io.igrant.mobileagent.events.ReceiveExchangeRequestEvent
import io.igrant.mobileagent.events.RefreshConnectionList
import io.igrant.mobileagent.indy.WalletManager
import io.igrant.mobileagent.models.MediatorConnectionObject
import org.greenrobot.eventbus.EventBus
import org.hyperledger.indy.sdk.non_secrets.WalletRecord

object DeleteUtils {

    private const val TAG = "DeleteUtils"
    fun deleteConnection(connectionId: String) {
        val connectionSearch = SearchUtils.searchWallet(
            WalletRecordType.CONNECTION,
            "{\"request_id\":\"$connectionId\"}"
        )
        if (connectionSearch.totalCount ?: 0 > 0) {

            //delete connection invitation
            val connectionInvitationSearch = SearchUtils.searchWallet(
                WalletRecordType.CONNECTION_INVITATION,
                "{\"connection_id\":\"${connectionSearch.records?.get(0)?.id}\"}"
            )

            for (record in connectionInvitationSearch.records ?: ArrayList()) {
                WalletRecord.delete(
                    WalletManager.getWallet,
                    WalletRecordType.CONNECTION_INVITATION,
                    "${record.id}"
                ).get()
            }
            //delete connection invitation

            val connectionObj = WalletManager.getGson.fromJson(
                connectionSearch.records?.get(0)?.value,
                MediatorConnectionObject::class.java
            )

            //delete did doc
            val didDocSearch = SearchUtils.searchWallet(
                WalletRecordType.DID_DOC,
                "{\"did\":\"${connectionObj.theirDid}\"}"
            )

            for (record in didDocSearch.records?:ArrayList()){
                WalletRecord.delete(
                    WalletManager.getWallet,
                    WalletRecordType.DID_DOC,
                    "${record.id}"
                ).get()
            }
            //delete did doc

            //delete did key
            val didKeySearch = SearchUtils.searchWallet(
                WalletRecordType.DID_KEY,
                "{\"did\":\"${connectionObj.theirDid}\"}"
            )

            for (record in didKeySearch.records?:ArrayList()){
                WalletRecord.delete(
                    WalletManager.getWallet,
                    WalletRecordType.DID_KEY,
                    "${record.id}"
                ).get()
            }
            //delete did key

            //delete Message record
            val messageRecordSearch = SearchUtils.searchWallet(
                WalletRecordType.MESSAGE_RECORDS,
                "{\"connectionId\":\"${connectionObj.requestId}\"}"
            )

            for (record in messageRecordSearch.records?:ArrayList()){
                WalletRecord.delete(
                    WalletManager.getWallet,
                    WalletRecordType.MESSAGE_RECORDS,
                    "${record.id}"
                ).get()
            }
            //delete message record

            //delete presentation exchange
            val exchangeDataSearch = SearchUtils.searchWallet(
                WalletRecordType.PRESENTATION_EXCHANGE_V10,
                "{\"connection_id\":\"${connectionObj.requestId}\"}"
            )

            for (record in exchangeDataSearch.records?:ArrayList()){
                WalletRecord.delete(
                    WalletManager.getWallet,
                    WalletRecordType.PRESENTATION_EXCHANGE_V10,
                    "${record.id}"
                ).get()
            }
            //delete presentation exchange

            //delete credential exchange
            val offerCredentialSearch = SearchUtils.searchWallet(
                WalletRecordType.CREDENTIAL_EXCHANGE_V10,
                "{\"connection_id\":\"${connectionObj.requestId}\"}"
            )

            for (record in offerCredentialSearch.records?:ArrayList()){
                WalletRecord.delete(
                    WalletManager.getWallet,
                    WalletRecordType.CREDENTIAL_EXCHANGE_V10,
                    "${record.id}"
                ).get()
            }
            //delete presentation exchange

            WalletRecord.delete(
                WalletManager.getWallet,
                WalletRecordType.CONNECTION,
                "${connectionSearch.records?.get(0)?.id}"
            ).get()

            Log.d(TAG, "deleteConnection: ")
        }


        EventBus.getDefault()
            .post(ReceiveExchangeRequestEvent())

        EventBus.getDefault()
            .post(RefreshConnectionList())

    }
}