package io.igrant.mobileagent.activty

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import io.igrant.mobileagent.R
import io.igrant.mobileagent.indy.LedgerNetworkType
import io.igrant.mobileagent.utils.LanguageUtils
import io.igrant.mobileagent.utils.LanguageUtils.LANG_ENGLISH
import io.igrant.mobileagent.utils.LocaleHelper


class SettingsActivity : BaseActivity() {

    lateinit var clLanguage: ConstraintLayout
    lateinit var tvLanguage: TextView
    lateinit var clNetwork: ConstraintLayout
    lateinit var tvNetwork: TextView
    lateinit var tvVersion:TextView
    private lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        initView()
        setUpToolbar()
        initListener()
        initValues()
    }

    private fun initValues() {
        try {
            val pInfo: PackageInfo =
                packageManager.getPackageInfo(packageName, 0)
            val version = pInfo.versionName
            tvVersion.text = resources.getString(R.string.txt_version_detail,version)
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()
        initChangingValues()
    }

    private fun initChangingValues() {
        tvNetwork.text = LedgerNetworkType.getSelectedNetworkName(this)
        tvLanguage.text = LanguageUtils.getLanguage(LocaleHelper.getLanguage(applicationContext)?:LANG_ENGLISH)
    }

    private fun initListener() {
        clLanguage.setOnClickListener {
            val intent = Intent(
                this,
                LanguageSelectionActivity::class.java
            )
            startActivity(intent)
        }

        clNetwork.setOnClickListener {
            val intent = Intent(
                this,
                LedgerNetworkListActivity::class.java
            )
            startActivity(intent)
        }
    }

    private fun initView() {
        clLanguage = findViewById(R.id.clLanguage)
        tvLanguage = findViewById(R.id.tvLanguage)
        clNetwork = findViewById(R.id.clNetwork)
        tvNetwork = findViewById(R.id.tvLedgerNetwork)
        tvVersion = findViewById(R.id.tvVersion)
        toolbar = findViewById(R.id.toolbar)
    }

    private fun setUpToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar!!.title = resources.getString(R.string.txt_settings)
        supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_arrow_back_black)
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