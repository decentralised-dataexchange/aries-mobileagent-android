package io.igrant.mobileagent.utils

object DidCommPrefixUtils {

    private const val PREFIX_1 = "did:sov:BzCbsNYhMrjHiqZDTUASHg;spec"
    const val PREFIX_2 = "https://didcomm.org"

    fun getType():String{
        return PREFIX_1
    }

    fun getType(type:String):String{
        if (type.contains(PREFIX_1)){
            return PREFIX_1
        }else if (type.contains(PREFIX_2)){
            return PREFIX_2
        }
        return PREFIX_1
    }
}