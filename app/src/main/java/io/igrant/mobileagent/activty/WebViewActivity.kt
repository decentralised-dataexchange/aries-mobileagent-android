package io.igrant.mobileagent.activty

import android.os.Bundle
import android.view.MenuItem
import android.webkit.WebView
import androidx.appcompat.widget.Toolbar
import io.igrant.mobileagent.R

class WebViewActivity : BaseActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var wvPage: WebView

    var pageTitle: String = ""
    var pageUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)
        getIntentData()
        initViews()
        setUpToolbar()
        initValues()
    }

    private fun initValues() {
        wvPage.loadUrl(pageUrl);
    }

    private fun getIntentData() {
        if (intent.extras != null) {
            pageTitle = intent.getStringExtra(EXTRA_PAGE_TITLE) ?: ""
            pageUrl = intent.getStringExtra(EXTRA_PAGE_URL) ?: ""
        }
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
        wvPage = findViewById(R.id.wvPage)
    }

    private fun setUpToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar!!.title = pageTitle
        supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_arrow_back_black)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

    companion object {
        const val EXTRA_PAGE_TITLE = "WebViewActivity.pageTitle"
        const val EXTRA_PAGE_URL = "WebViewActivity.pageUrl"
    }
}