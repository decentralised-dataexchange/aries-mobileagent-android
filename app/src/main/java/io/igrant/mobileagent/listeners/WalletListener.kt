package io.igrant.mobileagent.listeners

import io.igrant.mobileagent.models.certificateOffer.CertificateOffer
import io.igrant.mobileagent.models.walletSearch.Record

interface WalletListener {
    fun onDelete(id:String,position:Int)
}