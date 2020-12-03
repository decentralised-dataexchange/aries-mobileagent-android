package io.igrant.mobileagent.listeners

import io.igrant.mobileagent.models.certificateOffer.CertificateOffer

interface ConnectionMessageListener {
    fun onConnectionMessageClick(certificateOffer: CertificateOffer)
}