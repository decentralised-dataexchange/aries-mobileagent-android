package io.igrant.mobileagent.utils

import io.igrant.mobileagent.indy.WalletManager
import io.igrant.mobileagent.models.agentConfig.Invitation
import io.igrant.mobileagent.models.connectionRequest.DidDoc
import org.hyperledger.indy.sdk.crypto.Crypto
import java.nio.charset.StandardCharsets
import java.util.*


object PackingUtils {

    fun packMessage(
        recipientKey: String,
        routingKey: String,
        senderKey: String,
        message: String
    ): ByteArray {

        val primaryPacked = packMessage(recipientKey, senderKey, message)

        val primaryPackedString = String(primaryPacked, StandardCharsets.UTF_8)
        val forwardMessage = "{\n" +
                "        \"@type\": \"did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/routing/1.0/forward\",\n" +
                "        \"@id\": \"${UUID.randomUUID()}\",\n" +
                "        \"to\": \"$recipientKey\",\n" +
                "        \"msg\": \"$primaryPackedString\"\n" +
                "}"

        return packMessage(routingKey, senderKey, forwardMessage);
    }

    fun packMessage(invitation: Invitation, senderKey: String, message: String): ByteArray {

        val primaryPacked =
            packMessage(WalletManager.getGson.toJson(invitation.recipientKeys), senderKey, message)

        if (invitation.routingKeys != null && invitation.routingKeys?.size ?: 0 > 0) {
            val primaryPackedString = String(primaryPacked, StandardCharsets.UTF_8)
            val forwardMessage = "{\n" +
                    "        \"@type\": \"did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/routing/1.0/forward\",\n" +
                    "        \"@id\": \"${UUID.randomUUID()}\",\n" +
                    "        \"to\": \"${WalletManager.getGson.toJson(invitation.recipientKeys)}\",\n" +
                    "        \"msg\": \"$primaryPackedString\"\n" +
                    "}"
            return packMessage(
                WalletManager.getGson.toJson(invitation.routingKeys),
                senderKey,
                forwardMessage
            )
        } else {
            return primaryPacked
        }
    }

    fun packMessage(didDoc: DidDoc, senderKey: String, message: String): ByteArray {

            val primaryPacked =
                packMessage(
                    WalletManager.getGson.toJson(didDoc.service!![0].recipientKeys),
                    senderKey,
                    message
                )

            if (didDoc.service!![0].routingKeys != null && didDoc.service!![0].routingKeys?.size ?: 0 > 0) {
                val primaryPackedString = String(primaryPacked, StandardCharsets.UTF_8)
                val forwardMessage = "{\n" +
                        "        \"@type\": \"did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/routing/1.0/forward\",\n" +
                        "        \"@id\": \"${UUID.randomUUID()}\",\n" +
                        "        \"to\": \"${WalletManager.getGson.toJson(didDoc.service!![0].recipientKeys)}\",\n" +
                        "        \"msg\": \"$primaryPackedString\"\n" +
                        "}"
                return packMessage(
                    WalletManager.getGson.toJson(didDoc.service!![0].routingKeys),
                    senderKey,
                    forwardMessage
                )
            } else {
                return primaryPacked
            }
    }

    fun packMessage(recipientKey: String, senderKey: String, message: String): ByteArray {
        return Crypto.packMessage(
            WalletManager.getWallet,
            recipientKey,
            senderKey,
            message.toByteArray()
        ).get()
    }
}