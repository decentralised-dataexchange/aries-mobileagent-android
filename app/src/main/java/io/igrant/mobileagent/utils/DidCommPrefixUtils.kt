package io.igrant.mobileagent.utils

object DidCommPrefixUtils {

     const val MEDIATOR = "mediator"
     const val IGRANT_OPERATOR = "igrant_operator"
    private const val PREFIX_1 = "did:sov:BzCbsNYhMrjHiqZDTUASHg;spec"
    private const val PREFIX_2 = "https://didcomm.org"

    fun getType():String{
        return PREFIX_1
    }

    fun getType(type:String):String{
        when {
            type.contains(PREFIX_1) -> {
                return PREFIX_1
            }
            type.contains(PREFIX_2) -> {
                return PREFIX_2
            }
            type.contains(MEDIATOR) -> {
                return PREFIX_1
            }
            type.contains(IGRANT_OPERATOR) -> {
                return PREFIX_1
            }
            else -> return PREFIX_1
        }
    }
}