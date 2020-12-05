package io.igrant.mobileagent.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import io.igrant.mobileagent.R
import io.igrant.mobileagent.adapter.ConnectionListAdapter
import io.igrant.mobileagent.indy.WalletManager
import io.igrant.mobileagent.listeners.ConnectionClickListener
import io.igrant.mobileagent.utils.NavigationUtils
import io.igrant.mobileagent.utils.WalletRecordType
import org.hyperledger.indy.sdk.non_secrets.WalletSearch
import org.json.JSONArray
import org.json.JSONObject


class ConnectionListFragment : BaseFragment() {

    private lateinit var connectionRecords: JSONArray
    private lateinit var connectionRecordsCopy: JSONArray
    private lateinit var walletCertificateAdapter: ConnectionListAdapter
    private lateinit var rvConnections: RecyclerView
    private lateinit var llErrorMessage: LinearLayout
    private lateinit var btAddConnection: Button
    private lateinit var etSearch: EditText
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_connection_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        initListener()
        getConnectionList()
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
                override fun onConnectionClick(connection: String) {
                    NavigationUtils.showConnectionMessagesFragment(
                        parentFragmentManager,
                        connection
                    )
                }
            })
        rvConnections.adapter = walletCertificateAdapter
    }

    private fun initListener() {
        btAddConnection.setOnClickListener {

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

    private fun filterList(s: CharSequence?) {
        var tempList: ArrayList<JSONObject> = ArrayList()

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

        connectionRecords = JSONArray(tempList);

        walletCertificateAdapter.setList(connectionRecords)

    }

    private fun initViews(view: View) {
        rvConnections = view.findViewById(R.id.rvConnections)
        llErrorMessage = view.findViewById(R.id.llErrorMessage)
        btAddConnection = view.findViewById(R.id.btAddConnection)
        etSearch = view.findViewById(R.id.etSearch)
    }

    companion object {
        fun newInstance(): ConnectionListFragment {
            return ConnectionListFragment()
        }
    }

}