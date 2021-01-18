package io.igrant.mobileagent.listeners

import io.igrant.mobileagent.models.certificateOffer.CertificateOffer
import io.igrant.mobileagent.models.walletSearch.Record

interface ConnectionMessageListener {
    fun onConnectionMessageClick(record: Record,name:String)
}