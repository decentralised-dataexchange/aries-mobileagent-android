package io.igrant.mobileagent.utils

import io.igrant.mobileagent.indy.WalletManager
import io.igrant.mobileagent.models.agentConfig.ConfigPostResponse
import io.igrant.mobileagent.models.agentConfig.Invitation
import io.igrant.mobileagent.models.connectionRequest.DidDoc
import io.igrant.mobileagent.models.connectionRequest.ForwardMessage
import org.hyperledger.indy.sdk.crypto.Crypto
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.collections.ArrayList


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
                "        \"@type\": \"${DidCommPrefixUtils.getType()}/routing/1.0/forward\",\n" +
                "        \"@id\": \"${UUID.randomUUID()}\",\n" +
                "        \"to\": \"$recipientKey\",\n" +
                "        \"msg\": \"$primaryPackedString\"\n" +
                "}"

        return packMessage(routingKey, senderKey, forwardMessage);
    }

    fun packMessage(invitation: Invitation, senderKey: String, message: String): ByteArray {

        var primaryPacked =
            packMessage("[\"${invitation.recipientKeys?.get(0) ?: ""}\"]", senderKey, message)

        if (invitation.routingKeys != null && invitation.routingKeys?.size ?: 0 > 0) {
            for (s in invitation.routingKeys ?: ArrayList()) {
                val primaryPackedString =
                    String(primaryPacked, Charset.defaultCharset()).replace("\\u003d", "=")

                val forwardMessage: ForwardMessage = ForwardMessage()
                forwardMessage.type = "${DidCommPrefixUtils.getType()}/routing/1.0/forward"
                forwardMessage.id = UUID.randomUUID().toString()
                forwardMessage.to = invitation.recipientKeys?.get(0) ?: ""
                forwardMessage.msg = WalletManager.getGson.fromJson(
                    primaryPackedString.replace("\\u003d", "="),
                    ConfigPostResponse::class.java
                )

                primaryPacked = packMessage(
                    "[\"$s\"]",
                    senderKey,
                    WalletManager.getGson.toJson(forwardMessage)
                )
            }
            return primaryPacked
        } else {
            return primaryPacked
        }
    }

    fun packMessage(didDoc: DidDoc, senderKey: String, message: String): ByteArray {

        if (didDoc.service!![0].recipientKeys != null && didDoc.service!![0].recipientKeys?.size ?: 0 > 0) {
            var primaryPacked =
                packMessage(
                    "[\"${didDoc.service!![0].recipientKeys!![0]}\"]",
                    senderKey,
                    message
                )

            if (didDoc.service!![0].routingKeys != null && didDoc.service!![0].routingKeys?.size ?: 0 > 0) {

                for (s in didDoc.service!![0].routingKeys ?: ArrayList()) {
                    val primaryPackedString =
                        String(primaryPacked, Charset.defaultCharset()).replace("\\u003d", "=")

                    val forwardMessage: ForwardMessage = ForwardMessage()
                    forwardMessage.type = "${DidCommPrefixUtils.getType()}/routing/1.0/forward"
                    forwardMessage.id = UUID.randomUUID().toString()
                    forwardMessage.to = didDoc.service!![0].recipientKeys?.get(0) ?: ""
                    forwardMessage.msg = WalletManager.getGson.fromJson(
                        primaryPackedString.replace("\\u003d", "="),
                        ConfigPostResponse::class.java
                    )
                    primaryPacked = packMessage(
                        "[\"$s\"]",
                        senderKey,
                        WalletManager.getGson.toJson(forwardMessage)
                    )
                }

                return primaryPacked
            } else {
                return primaryPacked
            }
        } else {
            return ByteArray(0)
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