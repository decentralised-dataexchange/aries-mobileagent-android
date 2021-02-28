package io.igrant.mobileagent.dailogFragments

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.Glide
import com.kusu.loadingbutton.LoadingButton
import io.igrant.mobileagent.R
import io.igrant.mobileagent.communication.ApiManager
import io.igrant.mobileagent.events.ConnectionSuccessEvent
import io.igrant.mobileagent.handlers.CommonHandler
import io.igrant.mobileagent.indy.WalletManager
import io.igrant.mobileagent.models.MediatorConnectionObject
import io.igrant.mobileagent.models.agentConfig.ConfigPostResponse
import io.igrant.mobileagent.models.agentConfig.Invitation
import io.igrant.mobileagent.models.connection.Connection
import io.igrant.mobileagent.tasks.SaveConnectionTask
import io.igrant.mobileagent.tasks.SaveDidDocTask
import io.igrant.mobileagent.utils.DidCommPrefixUtils
import io.igrant.mobileagent.utils.PackingUtils
import io.igrant.mobileagent.utils.SearchUtils
import io.igrant.mobileagent.utils.WalletRecordType
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okio.BufferedSink
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.hyperledger.indy.sdk.crypto.Crypto
import org.hyperledger.indy.sdk.did.Did
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.IOException
import java.util.*

class ConnectionProgressDailogFragment : BaseDialogFragment() {

    private var isFromExchange: Boolean = false
    private var requestId: String? = ""
    private lateinit var invitation: Invitation
    private lateinit var proposal: String
    lateinit var btnConnect: LoadingButton
    lateinit var ivClose: ImageView
    lateinit var tvDesc: TextView
    lateinit var ivLogo: ImageView
    lateinit var tvName: TextView
    lateinit var llSuccess: LinearLayout
    lateinit var clItem: ConstraintLayout
    lateinit var pbLoader: ProgressBar
    lateinit var clConnection: ConstraintLayout
    lateinit var ivSuccess: ImageView

    var myDid = ""
    var myKey = ""
    var isIGrantEnabled = false
    var orgId = ""
    var location = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dailog_fragment_connection_progress, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isFromExchange = requireArguments().getBoolean("isFromExchange", false)
        invitation = requireArguments().getSerializable("invitation") as Invitation
        proposal = requireArguments().getString("proposal", "")
//        dialog!!.setTitle(title)

        initViews(view)
        tvDesc.text = Html.fromHtml(
            resources.getString(
                R.string.txt_allow_connection_query,
                invitation.label ?: "Organisation"
            )
        )
        tvName.text = invitation.label ?: "Organisation"

        Glide
            .with(ivLogo.context)
            .load(invitation?.image_url ?: invitation?.imageUrl ?: "")
            .centerCrop()
            .placeholder(R.drawable.images)
            .into(ivLogo)

        val myDidResult =
            Did.createAndStoreMyDid(WalletManager.getWallet, "{}").get()
        myDid = myDidResult.did
        myKey = myDidResult.verkey

        checkIfConnectionExisting()

        initListener(view)
    }

    /**
     * Function to check whether the connection is existing or not
     */
    private fun checkIfConnectionExisting() {

        val queryFeatureData = "{\n" +
                "    \"@type\": \"${DidCommPrefixUtils.getType()}/discover-features/1.0/query\",\n" +
                "    \"@id\": \"${UUID.randomUUID()}\",\n" +
                "    \"query\": \"${DidCommPrefixUtils.getType()}/igrantio-operator/*\",\n" +
                "    \"comment\": \"Querying features available.\",\n" +
                "    \"~transport\": {\n" +
                "        \"return_route\": \"all\"\n" +
                "    }\n" +
                "}"

        val queryFeaturePacked = PackingUtils.packMessage(invitation, myKey, queryFeatureData)

        val queryFeaturePackedBytes = object : RequestBody() {
            override fun contentType(): MediaType? {
                return "application/ssi-agent-wire".toMediaTypeOrNull()
            }

            @Throws(IOException::class)
            override fun writeTo(sink: BufferedSink) {
                sink.write(queryFeaturePacked)
            }
        }

        ApiManager.api.getService()
            ?.postData(invitation.serviceEndpoint ?: "", queryFeaturePackedBytes)
            ?.enqueue(object : Callback<ConfigPostResponse> {
                override fun onFailure(call: Call<ConfigPostResponse>, t: Throwable) {

                }

                override fun onResponse(
                    call: Call<ConfigPostResponse>,
                    response: Response<ConfigPostResponse>
                ) {
                    if (response.code() == 200 && response.body() != null) {
                        isIGrantEnabled = false
                        val unpack =
                            Crypto.unpackMessage(
                                WalletManager.getWallet,
                                WalletManager.getGson.toJson(response.body()).toString()
                                    .toByteArray()
                            ).get()

                        val dataArray =
                            JSONObject(JSONObject(String(unpack)).getString("message")).getJSONArray(
                                "protocols"
                            )

                        for (n in 0 until dataArray.length()) {
                            val obj = dataArray.getJSONObject(n)
                            if (obj.getString("pid").contains(
                                    "${DidCommPrefixUtils.getType()}/igrantio-operator",
                                    ignoreCase = true
                                )
                            ) {
                                isIGrantEnabled = true
                            }
                        }
                    }
                    getOrganizationDetailsIfNeeded()
                }
            })
    }

    private fun getOrganizationDetailsIfNeeded() {
        if (isIGrantEnabled) {
            requestId = UUID.randomUUID().toString()

            val orgData =
                "{ \"@type\": \"${DidCommPrefixUtils.getType()}/igrantio-operator/1.0/organization-info\", \"@id\": \"$requestId\" , \"~transport\": {" +
                        "\"return_route\": \"all\"}\n}"

            val orgDetailPacked = PackingUtils.packMessage(invitation, myKey, orgData)

            val orgDetailTypedArray = object : RequestBody() {
                override fun contentType(): MediaType? {
                    return "application/ssi-agent-wire".toMediaTypeOrNull()
                }

                @Throws(IOException::class)
                override fun writeTo(sink: BufferedSink) {
                    sink.write(orgDetailPacked)
                }
            }

            ApiManager.api.getService()
                ?.postData(invitation.serviceEndpoint ?: "", orgDetailTypedArray)
                ?.enqueue(object :
                    Callback<ConfigPostResponse> {
                    override fun onFailure(call: Call<ConfigPostResponse>, t: Throwable) {
                        Log.d("https", "onFailure: ")
                        pbLoader.visibility = View.GONE
                        Toast.makeText(
                            context,
                            resources.getString(R.string.err_unexpected),
                            Toast.LENGTH_SHORT
                        ).show()
                        dismiss()
                    }

                    override fun onResponse(
                        call: Call<ConfigPostResponse>,
                        response: Response<ConfigPostResponse>
                    ) {
                        if (response.code() == 200 && response.body() != null) {
                            val unpack =
                                Crypto.unpackMessage(
                                    WalletManager.getWallet,
                                    WalletManager.getGson.toJson(response.body()).toString()
                                        .toByteArray()
                                ).get()

                            val connectionData = WalletManager.getGson.fromJson(
                                JSONObject(String(unpack)).getString("message"),
                                Connection::class.java
                            )

                            orgId = connectionData.orgId ?: ""
                            location = connectionData.location?:""

                            var connectionListSearch =
                                SearchUtils.searchWallet(
                                    WalletRecordType.CONNECTION,
                                    "{\"orgId\":\"$orgId\"}"
                                )

                            if (connectionListSearch.totalCount ?: 0 > 0) {

                                val connectionObject = WalletManager.getGson.fromJson(
                                    connectionListSearch.records?.get(0)?.value,
                                    MediatorConnectionObject::class.java
                                )
                                sendDidToConnection(connectionObject.theirDid)

                                if (!isFromExchange)
                                    Toast.makeText(
                                        context,
                                        resources.getString(R.string.err_connection_already_added),
                                        Toast.LENGTH_SHORT
                                    ).show()

                            } else {
                                pbLoader.visibility = View.GONE
                                clConnection.visibility = View.VISIBLE
                            }
                        }
                    }
                })
        } else {
            pbLoader.visibility = View.GONE
            clConnection.visibility = View.VISIBLE
        }
    }

    private fun sendDidToConnection(theirDid: String?) {
        val data = "{\n" +
                "  \"@type\": \"${DidCommPrefixUtils.getType()}/igrantio-operator/1.0/org-multiple-connections\",\n" +
                "  \"@id\": \"${UUID.randomUUID()}\",\n" +
                "  \"theirdid\": \"${theirDid ?: ""}\"\n" +
                "}\n"

        val orgDetailPacked = PackingUtils.packMessage(invitation, myKey, data)

        val orgDetailTypedArray = object : RequestBody() {
            override fun contentType(): MediaType? {
                return "application/ssi-agent-wire".toMediaTypeOrNull()
            }

            @Throws(IOException::class)
            override fun writeTo(sink: BufferedSink) {
                sink.write(orgDetailPacked)
            }
        }

        ApiManager.api.getService()
            ?.postDataWithoutData(invitation.serviceEndpoint ?: "", orgDetailTypedArray)
            ?.enqueue(object :
                Callback<ResponseBody> {
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    dismiss()
                }

                override fun onResponse(
                    call: Call<ResponseBody>,
                    response: Response<ResponseBody>
                ) {
                    if (response.code() == 200 && response.body() != null) {

                        var connectionListSearch =
                            SearchUtils.searchWallet(
                                WalletRecordType.CONNECTION,
                                "{\"orgId\":\"$orgId\"}"
                            )

                        if (connectionListSearch.totalCount ?: 0 > 0) {
                            val connectionObject = WalletManager.getGson.fromJson(
                                connectionListSearch.records?.get(0)?.value,
                                MediatorConnectionObject::class.java
                            )
                            onSuccessListener.onExistingConnection(connectionObject.requestId ?: "")
                        }
                        dismiss()
                    }
                }

            })
    }

    private fun initListener(view: View) {

        ivClose.setOnClickListener {
            dialog?.dismiss()
        }

        btnConnect.setOnClickListener {
            btnConnect.isEnabled = false
            btnConnect.showLoading()

            SaveConnectionTask(object : CommonHandler {
                override fun taskStarted() {

                }

                override fun onSaveConnection(
                    typedBytes: RequestBody,
                    connectionRequest: RequestBody
                ) {

                    ApiManager.api.getService()?.cloudConnection(typedBytes)
                        ?.enqueue(object : Callback<ResponseBody> {
                            override fun onFailure(
                                call: Call<ResponseBody>,
                                t: Throwable
                            ) {

                            }

                            override fun onResponse(
                                call: Call<ResponseBody>,
                                response: Response<ResponseBody>
                            ) {
                                if (response.code() == 200 && response.body() != null) {
                                    ApiManager.api.getService()
                                        ?.postData(
                                            invitation.serviceEndpoint ?: "",
                                            connectionRequest
                                        )
                                        ?.enqueue(object :
                                            Callback<ConfigPostResponse> {
                                            override fun onFailure(
                                                call: Call<ConfigPostResponse>,
                                                t: Throwable
                                            ) {

                                            }

                                            override fun onResponse(
                                                call: Call<ConfigPostResponse>,
                                                response: Response<ConfigPostResponse>
                                            ) {
                                                if (response.code() == 200 && response.body() != null) {
                                                    SaveDidDocTask(
                                                        object : CommonHandler {
                                                            override fun taskStarted() {

                                                            }

                                                            override fun onSaveDidComplete(
                                                                typedBytes: RequestBody?,
                                                                serviceEndPoint: String
                                                            ) {
                                                                if (typedBytes != null)
                                                                    ApiManager.api.getService()
                                                                        ?.postDataWithoutData(
                                                                            serviceEndPoint,
                                                                            typedBytes
                                                                        )
                                                                        ?.enqueue(object :
                                                                            Callback<ResponseBody> {
                                                                            override fun onFailure(
                                                                                call: Call<ResponseBody>,
                                                                                t: Throwable
                                                                            ) {

                                                                            }

                                                                            override fun onResponse(
                                                                                call: Call<ResponseBody>,
                                                                                response: Response<ResponseBody>
                                                                            ) {

                                                                            }
                                                                        })
                                                            }
                                                        },
                                                        WalletManager.getGson.toJson(
                                                            response.body()
                                                        ), isIGrantEnabled
                                                    ).execute()
                                                }
                                            }
                                        })
                                }
                            }
                        })
                }
            }, invitation).execute(myDid, myKey, orgId, requestId,location)
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onConnectionSuccessEvent(event: ConnectionSuccessEvent) {
        btnConnect.hideLoading()
        btnConnect.isEnabled = true
        llSuccess.visibility = View.VISIBLE
        Handler(Looper.getMainLooper()).postDelayed({

//            GetConnectionDetailTask().execute(event.connectionId)
            onSuccessListener.onSuccess(proposal, orgId)
            llSuccess.visibility = View.GONE
            dialog?.dismiss()
        }, 3000)

    }

    override fun onStart() {
        try {
            EventBus.getDefault().register(this)
        } catch (e: Exception) {
        }
        super.onStart()
    }

    override fun onStop() {
        try {
            EventBus.getDefault().unregister(this)
        } catch (e: Exception) {
        }
        super.onStop()
    }

    private fun initViews(view: View) {
        btnConnect = view.findViewById(R.id.btnConnect)
        ivClose = view.findViewById(R.id.ivClose)
        tvDesc = view.findViewById(R.id.tvDesc)
        ivLogo = view.findViewById(R.id.ivLogo)
        tvName = view.findViewById(R.id.tvName)
        llSuccess = view.findViewById(R.id.llSuccess)
        ivSuccess = view.findViewById(R.id.ivSuccess)
        pbLoader = view.findViewById(R.id.pbLoader)
        clItem = view.findViewById(R.id.clItem)
        clConnection = view.findViewById(R.id.clConnection)
    }

    companion object {
        fun newInstance(
            isFromExchange: Boolean,
            invitation: Invitation,
            proposal: String
        ): ConnectionProgressDailogFragment {
            val fragment = ConnectionProgressDailogFragment()
            val args = Bundle()
            args.putBoolean("isFromExchange", isFromExchange)
            args.putSerializable("invitation", invitation)
            args.putString("proposal", proposal)
            fragment.arguments = args
            return fragment
        }
    }

    lateinit var onSuccessListener: OnConnectionSuccess

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            onSuccessListener = context as OnConnectionSuccess
        } catch (e: Exception) {
        }
    }

    interface OnConnectionSuccess {
        fun onSuccess(proposal: String, connectionId: String)
        fun onExistingConnection(connectionId: String) {}
    }

}