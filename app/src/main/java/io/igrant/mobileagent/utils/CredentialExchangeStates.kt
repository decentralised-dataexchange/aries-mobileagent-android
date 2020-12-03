package io.igrant.mobileagent.utils

class CredentialExchangeStates {

    companion object{
        const val CREDENTIAL_OFFER_RECEIVED ="offer_received"
        const val CREDENTIAL_CREDENTIAL_RECEIVED ="credential_received"
        const val CREDENTIAL_REQUEST_SENT ="request_sent"
        const val CREDENTIAL_CREDENTIAL_ACK ="credential_acked"
    }
}