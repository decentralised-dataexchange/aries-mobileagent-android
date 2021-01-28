package io.igrant.mobileagent.activty

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import io.igrant.mobileagent.R
import io.igrant.mobileagent.activty.WebViewActivity.Companion.EXTRA_PAGE_TITLE
import io.igrant.mobileagent.activty.WebViewActivity.Companion.EXTRA_PAGE_URL
import java.lang.annotation.RetentionPolicy

class AboutAppActivity:BaseActivity() {

    private lateinit var toolbar: Toolbar
    lateinit var tvVersion: TextView
    lateinit var tvAriesProtocols:TextView
    lateinit var tvTermsAndConditions:TextView
    lateinit var tvPrivacyPolicy: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_app)
        initViews()
        setUpToolbar()
        initValues()
        initListeners()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        tvVersion = findViewById(R.id.tvVersion)
        tvAriesProtocols = findViewById(R.id.tvAriesProtocols)
        tvTermsAndConditions = findViewById(R.id.tvTermsAndConditions)
        tvPrivacyPolicy = findViewById(R.id.tvPrivacyPolicy)
    }

    private fun setUpToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar!!.title = resources.getString(R.string.title_about_this_app)
        supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_arrow_back_black)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
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

    private fun initListeners(){
        tvPrivacyPolicy.setOnClickListener {
            val intent = Intent(
                this,
                WebViewActivity::class.java
            )
            intent.putExtra(EXTRA_PAGE_TITLE,resources.getString(R.string.txt_privacy_policy))
            intent.putExtra(EXTRA_PAGE_URL,"https://datawallet.igrant.io/wallet-privacy.html")
            startActivity(intent)
        }

        tvTermsAndConditions.setOnClickListener {
            val intent = Intent(
                this,
                WebViewActivity::class.java
            )
            intent.putExtra(EXTRA_PAGE_TITLE,resources.getString(R.string.txt_terms_and_conditions))
            intent.putExtra(EXTRA_PAGE_URL,"https://datawallet.igrant.io/wallet-terms.html")
            startActivity(intent)
        }

        tvAriesProtocols.setOnClickListener {
            val intent = Intent(
                this,
                WebViewActivity::class.java
            )
            intent.putExtra(EXTRA_PAGE_TITLE,resources.getString(R.string.txt_supported_protocols))
            intent.putExtra(EXTRA_PAGE_URL,"https://datawallet.igrant.io/aries-supported-protocols.html")
            startActivity(intent)
        }
    }
}