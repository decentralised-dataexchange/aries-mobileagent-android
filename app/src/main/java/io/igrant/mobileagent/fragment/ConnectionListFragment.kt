package io.igrant.mobileagent.fragment

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.igrant.mobileagent.R
import io.igrant.mobileagent.activty.ConnectionDetailActivity
import io.igrant.mobileagent.activty.InitializeActivity
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

    private var toolbarTitle: String? = ""

    override fun onResume() {
        toolbarTitle = (activity as InitializeActivity?)?.getActionBarTitle()
        (activity as InitializeActivity?)?.setActionBarTitle(getString(R.string.title_connection_list))
        super.onResume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as InitializeActivity?)?.setActionBarTitle(toolbarTitle)
    }

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
                override fun onConnectionClick(connection: String, did: String) {
                    val clipboard: ClipboardManager? =
                        requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
                    val clip = ClipData.newPlainText("DID", did)
                    clipboard?.setPrimaryClip(clip)
                    val intent = Intent(requireContext(),ConnectionDetailActivity::class.java)
                    intent.putExtra(ConnectionDetailActivity.EXTRA_CONNECTION_DATA,connection)
                    startActivity(intent)
//                    NavigationUtils.showConnectionMessagesFragment(
//                        parentFragmentManager,
//                        connection
//                    )
                }
            })
        rvConnections.layoutManager = GridLayoutManager(context, 3)
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