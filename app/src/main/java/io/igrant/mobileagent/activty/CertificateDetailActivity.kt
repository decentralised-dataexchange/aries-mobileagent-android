package io.igrant.mobileagent.activty

import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.igrant.mobileagent.R
import io.igrant.mobileagent.adapter.CertificateAttributeAdapter
import io.igrant.mobileagent.indy.WalletManager
import io.igrant.mobileagent.models.presentationExchange.PresentationExchange
import io.igrant.mobileagent.models.wallet.WalletModel
import io.igrant.mobileagent.models.walletSearch.Record

class CertificateDetailActivity:BaseActivity() {

    private lateinit var adapter: CertificateAttributeAdapter
    private var wallet: WalletModel? = null

    private lateinit var toolbar: Toolbar
    private lateinit var rvAttributes: RecyclerView
    private lateinit var tvHead: TextView

    companion object{
        const val EXTRA_WALLET_DETAIL = "CertificateDetailActivity.wallet"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_certificate_detail)
        getIntentData()
        initViews()
        initValues()
        initListener()
        setUpToolbar()
        setUpAdapter()
    }

    private fun getIntentData() {
        val wal = intent.extras!!.getString(EXTRA_WALLET_DETAIL)
        wallet = WalletManager.getGson.fromJson(wal,WalletModel::class.java)
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        rvAttributes = findViewById(R.id.rvAttributes)
        tvHead = findViewById(R.id.tvHead)
    }

    private fun initValues() {
        try {
            tvHead.text = ((wallet?.rawCredential?.schemaId?:"").split(":")[2]).toUpperCase()
        } catch (e: Exception) {
        }
    }

    private fun initListener() {

    }

    private fun setUpToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar!!.title = ""
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

    private fun setUpAdapter() {
        adapter = CertificateAttributeAdapter(
            wallet?.credentialProposalDict?.credentialProposal?.attributes ?: ArrayList()
        )
        rvAttributes.layoutManager = LinearLayoutManager(this)
        rvAttributes.adapter = adapter
    }

}