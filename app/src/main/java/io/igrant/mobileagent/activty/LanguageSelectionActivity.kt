package io.igrant.mobileagent.activty

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.LinearLayout
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.igrant.mobileagent.R
import io.igrant.mobileagent.adapter.LanguageAdapter
import io.igrant.mobileagent.listeners.LanguageClickListener
import io.igrant.mobileagent.models.Language
import io.igrant.mobileagent.utils.LanguageUtils
import io.igrant.mobileagent.utils.LocaleHelper
import java.util.*

class LanguageSelectionActivity : BaseActivity() {

    private lateinit var adapter: LanguageAdapter
    private lateinit var toolbar: Toolbar
    private lateinit var rvAttributes: RecyclerView
    private lateinit var llProgressBar: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language_selection)
        initViews()
        setUpToolbar()
        initValues()
    }

    private fun initValues() {
        val list: ArrayList<Language> = LanguageUtils.getLanguageList(applicationContext)

        adapter = LanguageAdapter(
            list,
            object : LanguageClickListener{
                override fun onLanguageClick(languageCode: String) {
                    LocaleHelper.setLocale(this@LanguageSelectionActivity, languageCode)
                    val locale = Locale(languageCode)
                    Locale.setDefault(locale)
                    reloadActivity()
                }
            })
        rvAttributes.layoutManager = LinearLayoutManager(this)
        rvAttributes.adapter = adapter
    }

    fun reloadActivity() {
        val intent = Intent(this@LanguageSelectionActivity, InitializeActivity::class.java)
        //        overridePendingTransition(R.anim.slide_left_out, R.anim.slide_right_in);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        finish()
        startActivity(intent)
    }
    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        rvAttributes = findViewById(R.id.rvAttributes)
        llProgressBar = findViewById(R.id.llProgressBar)
    }

    private fun setUpToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar!!.title = resources.getString(R.string.txt_language)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
            }
            else -> {

            }
        }
        return super.onOptionsItemSelected(item)
    }

}