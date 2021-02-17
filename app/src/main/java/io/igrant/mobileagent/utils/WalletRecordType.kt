package io.igrant.mobileagent.utils

class WalletRecordType {
    companion object{
        const val MEDIATOR_CONNECTION ="mediator_connection"
        const val MEDIATOR_CONNECTION_INVITATION ="mediator_connection_invitation"

        const val CONNECTION ="connection"
        const val CONNECTION_INVITATION ="connection_invitation"

        const val MEDIATOR_DID_DOC ="mediator_did_doc"
        const val MEDIATOR_DID_KEY ="mediator_did_key"

        const val DID_DOC ="did_doc"
        const val DID_KEY ="did_key"

        const val CREDENTIAL_EXCHANGE_V10 = "credential_exchange_v10"

        const val MESSAGE_RECORDS = "inbox_messages"

        const val PRESENTATION_EXCHANGE_V10 = "presentation_exchange_v10"

        const val WALLET ="wallet"
    }
}