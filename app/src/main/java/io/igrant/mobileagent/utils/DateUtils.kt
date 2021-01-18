package io.igrant.mobileagent.utils

import android.text.format.DateUtils
import java.text.SimpleDateFormat
import java.util.*

object DateUtils {

    private const val INDY_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSSSSS'Z'"

    fun getIndyFormattedDate(): String {
        val sdf = SimpleDateFormat(INDY_DATE_FORMAT)
        return sdf.format(Date())
//        Log.d(TAG, "onCreate: date ::::: $date")
    }

    fun getRelativeTime(date:String):String{
        val sdf = SimpleDateFormat(
            INDY_DATE_FORMAT,
            Locale.ENGLISH
        )
        var dDate: Date?
        dDate = try {
            sdf.parse(date)
        } catch (e: Exception) {
            return "nil"
        }
        return DateUtils.getRelativeTimeSpanString(dDate.time).toString()
    }
}