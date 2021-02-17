package io.igrant.mobileagent.activty

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.igrant.mobileagent.R
import io.igrant.mobileagent.adapter.LedgerNetworkAdapter
import io.igrant.mobileagent.handlers.PoolHandler
import io.igrant.mobileagent.indy.LedgerNetworkType
import io.igrant.mobileagent.indy.PoolManager
import io.igrant.mobileagent.listeners.LedgerNetworkClickListener
import io.igrant.mobileagent.models.ledger.LedgerItem
import io.igrant.mobileagent.tasks.PoolTask
import org.hyperledger.indy.sdk.pool.Pool

class LedgerNetworkListActivity : BaseActivity() {

    private lateinit var adapter: LedgerNetworkAdapter
    private lateinit var toolbar: Toolbar
    private lateinit var rvAttributes: RecyclerView
    private lateinit var llProgressBar: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ledger_network_list)
        initViews()
        setUpToolbar()
        initValues()
    }

    private fun initValues() {
        val list: ArrayList<LedgerItem> = ArrayList()

        var ledger = LedgerItem()
        ledger.name = "iGrant.io Sandbox"
        ledger.type = LedgerNetworkType.IGRANT_SANDBOX
        list.add(ledger)

        ledger = LedgerItem()
        ledger.name = "Sovrin Builder"
        ledger.type = LedgerNetworkType.SOVRIN_BUILDER
        list.add(ledger)

        ledger = LedgerItem()
        ledger.name = "Sovrin Live"
        ledger.type = LedgerNetworkType.SOVRIN_LIVE
        list.add(ledger)

        ledger = LedgerItem()
        ledger.name = "Sovrin Sandbox"
        ledger.type = LedgerNetworkType.SOVRIN_SANDBOX
        list.add(ledger)

        adapter = LedgerNetworkAdapter(
            list,
            LedgerNetworkType.getSelectedNetwork(this),
            object : LedgerNetworkClickListener {
                override fun onNetworkClick(networkType: Int) {
                    llProgressBar.visibility = View.VISIBLE
                    LedgerNetworkType.saveSelectedNetwork(
                        this@LedgerNetworkListActivity,
                        networkType
                    )

                    PoolManager.getPool?.close()
                    PoolManager.removePool

                    PoolTask(
                        object : PoolHandler {
                            override fun taskCompleted(pool: Pool) {
                                PoolManager.setPool(pool)
                                adapter.setType(LedgerNetworkType.getSelectedNetwork(this@LedgerNetworkListActivity))
                                llProgressBar.visibility = View.GONE
                            }

                            override fun taskStarted() {

                            }
                        },
                        LedgerNetworkType.getSelectedNetwork(this@LedgerNetworkListActivity)
                    ).execute()
                }
            })
        rvAttributes.layoutManager = LinearLayoutManager(this)
        rvAttributes.adapter = adapter
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        rvAttributes = findViewById(R.id.rvAttributes)
        llProgressBar = findViewById(R.id.llProgressBar)
    }

    private fun setUpToolbar() {
        setSupportActionBar(toolbar)
        supportActionBar!!.title = resources.getString(R.string.txt_network)
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