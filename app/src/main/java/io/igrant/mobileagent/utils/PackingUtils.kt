package io.igrant.mobileagent.utils

import io.igrant.mobileagent.indy.WalletManager
import org.hyperledger.indy.sdk.crypto.Crypto
import java.nio.charset.StandardCharsets
import java.util.*


object PackingUtils {

    fun packMessage(recipientKey:String, routingKey:String, senderKey:String, message:String): ByteArray {

        val primaryPacked = packMessage(recipientKey,senderKey,message)

        val primaryPackedString = String(primaryPacked, StandardCharsets.UTF_8)
        val forwardMessage = "{\n" +
                "        \"@type\": \"did:sov:BzCbsNYhMrjHiqZDTUASHg;spec/routing/1.0/forward\",\n" +
                "        \"@id\": \"${UUID.randomUUID()}\",\n" +
                "        \"to\": \"$recipientKey\",\n" +
                "        \"msg\": \"$primaryPackedString\"\n" +
                "}"

        return packMessage(routingKey,senderKey,forwardMessage);
    }

    fun packMessage(recipientKey:String, senderKey:String, message:String):ByteArray{
        return Crypto.packMessage(
            WalletManager.getWallet,
            recipientKey,
            senderKey,
            message.toByteArray()
        ).get();
    }
}