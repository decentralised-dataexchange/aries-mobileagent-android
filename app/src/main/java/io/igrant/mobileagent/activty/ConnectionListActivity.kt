package io.igrant.mobileagent.activty

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.igrant.mobileagent.R
import io.igrant.mobileagent.adapter.ConnectionListAdapter
import io.igrant.mobileagent.dailogFragments.ConnectionProgressDailogFragment
import io.igrant.mobileagent.events.ReceiveExchangeRequestEvent
import io.igrant.mobileagent.events.RefreshConnectionList
import io.igrant.mobileagent.indy.WalletManager
import io.igrant.mobileagent.listeners.ConnectionClickListener
import io.igrant.mobileagent.models.agentConfig.Invitation
import io.igrant.mobileagent.qrcode.QrCodeActivity
import io.igrant.mobileagent.utils.ConnectionUtils
import io.igrant.mobileagent.utils.DeleteUtils
import io.igrant.mobileagent.utils.PermissionUtils
import io.igrant.mobileagent.utils.WalletRecordType
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.hyperledger.indy.sdk.non_secrets.WalletSearch
import org.json.JSONArray
import org.json.JSONObject


class ConnectionListActivity : BaseActivity(),
    ConnectionProgressDailogFragment.OnConnectionSuccess {

    private lateinit var connectionRecords: JSONArray
    private lateinit var connectionRecordsCopy: JSONArray
    private lateinit var walletCertificateAdapter: ConnectionListAdapter
    private lateinit var rvConnections: RecyclerView
    private lateinit var llErrorMessage: LinearLayout
    private lateinit var etSearch: EditText
    private lateinit var toolbar: Toolbar
    private lateinit var ivAdd: ImageView

    companion object {
        private const val PICK_IMAGE_REQUEST = 101
        val PERMISSIONS =
            arrayOf(Manifest.permission.CAMERA)
        private const val REQUEST_CODE_SCAN_INVITATION = 202

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connection_list)
        initViews()
        setUpToolbar()
        initListener()
        getConnectionList()
        try {
            EventBus.getDefault().register(this)
        } catch (e: Exception) {
        }
    }

    private fun setUpToolbar() {
        setSupportActionBar(toolbar)
//        supportActionBar!!.title = resources.getString(R.string.title_connection_list)
        supportActionBar!!.title = ""
        supportActionBar!!.setHomeAsUpIndicator(R.drawable.ic_arrow_back_black)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
    }

//    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
//        menuInflater.inflate(R.menu.main_menu, menu)
//        return true
//    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
//            R.id.action_new -> {
//                if (PermissionUtils.hasPermissions(
//                        this,
//                        true,
//                        PICK_IMAGE_REQUEST,
//                        PERMISSIONS
//                    )
//                ) {
//                    val i = Intent(this, QrCodeActivity::class.java)
//                    startActivityForResult(
//                        i,
//                        REQUEST_CODE_SCAN_INVITATION
//                    )
//                }
//                true
//            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CODE_SCAN_INVITATION) {
            if (data == null) return

            val uri: Uri = try {
                Uri.parse(data.getStringExtra("com.blikoon.qrcodescanner.got_qr_scan_relult"))
            } catch (e: Exception) {
                Uri.parse("igrant.io")
            }
            val v: String = uri.getQueryParameter("c_i") ?: ""
            if (v != "") {
                saveConnection(v)
            } else {
                Toast.makeText(
                    this,
                    resources.getString(R.string.err_unexpected),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun saveConnection(data: String) {
        var invitation: Invitation? = null
        try {
            val json =
                Base64.decode(
                    data,
                    Base64.URL_SAFE
                ).toString(charset("UTF-8"))
            invitation = WalletManager.getGson.fromJson(json, Invitation::class.java)
        } catch (e: Exception) {
        }

        if (invitation != null) {
            if (ConnectionUtils.checkIfConnectionAvailable(invitation.recipientKeys!![0])) {
                Toast.makeText(
                    this,
                    resources.getString(R.string.err_connection_already_added),
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                Handler(Looper.getMainLooper()).postDelayed({
                    val connectionSuccessDialogFragment: ConnectionProgressDailogFragment =
                        ConnectionProgressDailogFragment.newInstance(
                            "${invitation.label ?: ""} has invited you to connect",
                            invitation,
                            ""
                        )
                    connectionSuccessDialogFragment.show(
                        supportFragmentManager,
                        "fragment_edit_name"
                    )
                }, 200)
            }
        } else {
            Toast.makeText(this, resources.getString(R.string.err_unexpected), Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun getConnectionList() {
        val search = WalletSearch.open(
            WalletManager.getWallet,
            WalletRecordType.CONNECTION,
            "{}",
            "{ \"retrieveRecords\": true, \"retrieveTotalCount\": true, \"retrieveType\": false, \"retrieveValue\": true, \"retrieveTags\": true }"
        ).get()

        val connection =
            WalletSearch.searchFetchNextRecords(WalletManager.getWallet, search, 100).get()

        WalletSearch.closeSearch(search)
        val data = JSONObject(connection)
        if (data.getInt("totalCount") > 0) {
            llErrorMessage.visibility = View.GONE
            connectionRecords = JSONArray(data.get("records").toString())
            connectionRecordsCopy = JSONArray(data.get("records").toString())
            setUpCertificateList()
        } else {
            llErrorMessage.visibility = View.VISIBLE
        }
    }

    private fun setUpCertificateList() {
        walletCertificateAdapter =
            ConnectionListAdapter(connectionRecords, object :
                ConnectionClickListener {
                override fun onConnectionClick(connection: String, did: String) {
                    val clipboard: ClipboardManager? =
                        getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
                    val clip = ClipData.newPlainText("DID", did)
                    clipboard?.setPrimaryClip(clip)
                    val intent =
                        Intent(this@ConnectionListActivity, ConnectionDetailActivity::class.java)
                    intent.putExtra(ConnectionDetailActivity.EXTRA_CONNECTION_DATA, connection)
                    startActivity(intent)

//                    DeleteUtils.deleteConnection(connection)
                }
            })
        rvConnections.layoutManager = GridLayoutManager(this, 3)
        rvConnections.adapter = walletCertificateAdapter
    }

    private fun initListener() {
        ivAdd.setOnClickListener {
            if (PermissionUtils.hasPermissions(
                    this,
                    true,
                    PICK_IMAGE_REQUEST,
                    PERMISSIONS
                )
            ) {
                val i = Intent(this, QrCodeActivity::class.java)
                startActivityForResult(
                    i,
                    REQUEST_CODE_SCAN_INVITATION
                )
            }
        }

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterList(s)
            }
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            val i = Intent(this, QrCodeActivity::class.java)
            startActivityForResult(
                i,
                REQUEST_CODE_SCAN_INVITATION
            )
        }
    }

    private fun filterList(s: CharSequence?) {
        val tempList: ArrayList<JSONObject> = ArrayList()

        for (i in 0 until connectionRecordsCopy.length()) {
            try {
                val title = JSONObject(
                    connectionRecordsCopy.getJSONObject(i).getString("value")
                ).getString("their_label")
                if (title.contains(s ?: "", ignoreCase = true)) {
                    tempList.add(connectionRecordsCopy.getJSONObject(i))
                }
            } catch (e: Exception) {
            }
        }

        connectionRecords = JSONArray(tempList)

        walletCertificateAdapter.setList(connectionRecords)

    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        rvConnections = findViewById(R.id.rvConnections)
        llErrorMessage = findViewById(R.id.llErrorMessage)
        etSearch = findViewById(R.id.etSearch)
        ivAdd = findViewById(R.id.ivAdd)
    }

    override fun onSuccess(proposal: String, connectionId: String) {
        getConnectionList()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            EventBus.getDefault().unregister(this)
        } catch (e: Exception) {
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun refreshList(event: RefreshConnectionList) {
        getConnectionList()
    }

}