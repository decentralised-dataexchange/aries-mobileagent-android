package io.igrant.mobileagent.dailogFragments

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.kusu.loadingbutton.LoadingButton
import io.igrant.mobileagent.R
import io.igrant.mobileagent.communication.ApiManager
import io.igrant.mobileagent.events.ConnectionSuccessEvent
import io.igrant.mobileagent.handlers.CommonHandler
import io.igrant.mobileagent.indy.WalletManager
import io.igrant.mobileagent.models.agentConfig.ConfigPostResponse
import io.igrant.mobileagent.models.agentConfig.Invitation
import io.igrant.mobileagent.tasks.SaveConnectionTask
import io.igrant.mobileagent.tasks.SaveDidDocTask
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ConnectionProgressDailogFragment : BaseDialogFragment() {

    private lateinit var invitation: Invitation
    private lateinit var proposal:String
    lateinit var btnConnect: LoadingButton
    lateinit var btnDecline: LoadingButton
    lateinit var tvDesc: TextView
    lateinit var ivLogo: ImageView
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dailog_fragment_connection_progress, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val title = requireArguments().getString("title", "")
        invitation = requireArguments().getSerializable("invitation") as Invitation
        proposal = requireArguments().getString("proposal","")
//        dialog!!.setTitle(title)

        initViews(view)
        tvDesc.text = title
        Glide
            .with(ivLogo.context)
            .load(invitation.imageUrl)
            .centerCrop()
            .placeholder(R.drawable.images)
            .into(ivLogo)
        initListener(view)
    }

    private fun initListener(view: View) {

        btnDecline.setOnClickListener {
            dialog?.dismiss()
        }
        btnConnect.setOnClickListener {

            btnConnect.showLoading()
            Toast.makeText(
                context,
                "Connecting to connection. Will update when the connection is active...",
                Toast.LENGTH_SHORT
            ).show()

            SaveConnectionTask(object : CommonHandler {
                override fun taskStarted() {

                }

                override fun onSaveConnection(
                    typedBytes: RequestBody,
                    connectionRequest: RequestBody
                ) {
                    ApiManager.api.getService()?.cloudConnection(typedBytes)
                        ?.enqueue(object : Callback<ResponseBody> {
                            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {

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
                                        ?.enqueue(object : Callback<ConfigPostResponse> {
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
                                                                typedBytes: RequestBody,
                                                                serviceEndPoint: String
                                                            ) {
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
                                                        WalletManager.getGson.toJson(response.body())
                                                    ).execute()
                                                }
                                            }
                                        })
                                }
                            }
                        })
                }
            }, invitation).execute()
        }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun onConnectionSuccessEvent(event: ConnectionSuccessEvent) {
        btnConnect.hideLoading()
        btnDecline.visibility = View.INVISIBLE
        btnConnect.visibility = View.INVISIBLE
        ivLogo.visibility = View.INVISIBLE
        tvDesc.text = resources.getString(R.string.txt_connection_success_message)
        Handler(Looper.getMainLooper()).postDelayed({
            onSuccessListener.onSuccess(proposal,event.connectionId?:"")
            dialog?.dismiss()
        }, 1000)

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
        btnDecline = view.findViewById(R.id.btnDecline)
        tvDesc = view.findViewById(R.id.tvDesc)
        ivLogo = view.findViewById(R.id.ivLogo)
    }

    companion object {
        fun newInstance(
            title: String,
            invitation: Invitation,
            proposal: String
        ): ConnectionProgressDailogFragment {
            val fragment = ConnectionProgressDailogFragment()
            val args = Bundle()
            args.putString("title", title)
            args.putSerializable("invitation", invitation)
            args.putString("proposal",proposal)
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
    }
}