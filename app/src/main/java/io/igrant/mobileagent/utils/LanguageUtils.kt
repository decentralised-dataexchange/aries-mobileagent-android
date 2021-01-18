package io.igrant.mobileagent.utils

import android.content.Context
import android.widget.TextView
import io.igrant.mobileagent.models.Language
import java.util.*

object LanguageUtils {
    const val LANG_ENGLISH = "en"
    const val LANG_SWEDISH = "sv"
    const val LANG_PORTUGUESE = "pt"
    private const val LANG_ENGLISH_STRING = "English"
    private const val LANG_SWIDISH_STRING = "Svenska"
    private const val LANG_PORTUGUESE_STRING = "Português"
    private const val KEY_USER_LANGUAGE = "key_user_app_language"

    fun getLanguageList(context: Context): ArrayList<Language> {

        val lang: String = LocaleHelper.getLanguage(context) ?: "en"

        val languages: ArrayList<Language> = ArrayList<Language>()
        var language = Language()
        language.language =LANG_ENGLISH_STRING
        language.languageCode = LANG_ENGLISH
        language.isChecked = lang == LANG_ENGLISH
        languages.add(language)

        language = Language()
        language.language = LANG_SWIDISH_STRING
        language.languageCode = LANG_SWEDISH
        language.isChecked = lang == LANG_SWEDISH
        languages.add(language)

        return languages
    }

    fun getLanguage(code:String): String {

        when(code){
            LANG_ENGLISH->{
                return LANG_ENGLISH_STRING
            }
            LANG_SWEDISH->{
                return LANG_SWIDISH_STRING
            }
            else->{
                return LANG_ENGLISH_STRING
            }
        }
    }

    fun setLanguageValue(tvLanguageName: TextView, context: Context?) {
        if (LocaleHelper.getCurrentLocale(context!!).language
                .equals(LANG_ENGLISH)
        ) {
            tvLanguageName.text = LANG_ENGLISH_STRING
        } else {
            tvLanguageName.text = LANG_SWIDISH_STRING
        }
    }
}
