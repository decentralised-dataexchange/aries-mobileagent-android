package io.igrant.mobileagent.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import io.igrant.mobileagent.listeners.ConnectionClickListener
import io.igrant.mobileagent.R
import io.igrant.mobileagent.adapter.ConnectionListAdapter
import io.igrant.mobileagent.indy.WalletManager
import io.igrant.mobileagent.utils.NavigationUtils
import io.igrant.mobileagent.utils.WalletRecordType
import org.hyperledger.indy.sdk.non_secrets.WalletSearch
import org.json.JSONArray
import org.json.JSONObject

class ConnectionListFragment : BaseFragment() {

    private lateinit var walletCertificateAdapter: ConnectionListAdapter
    private lateinit var rvConnections: RecyclerView
    private lateinit var llErrorMessage:LinearLayout
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
            var connectionRecords: JSONArray = JSONArray(data.get("records").toString())
            setUpCertificateList(connectionRecords)
        }else{
            llErrorMessage.visibility = View.VISIBLE
        }
    }

    private fun setUpCertificateList(connectionRecords: JSONArray) {
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

    }

    private fun initViews(view: View) {
        rvConnections = view.findViewById(R.id.rvConnections)
        llErrorMessage = view.findViewById(R.id.llErrorMessage)
    }

    companion object {
        fun newInstance(): ConnectionListFragment {
            return ConnectionListFragment()
        }
    }

}