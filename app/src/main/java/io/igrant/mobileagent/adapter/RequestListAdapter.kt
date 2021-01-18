package io.igrant.mobileagent.adapter

import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import io.igrant.mobileagent.R
import io.igrant.mobileagent.indy.WalletManager
import io.igrant.mobileagent.listeners.ConnectionMessageListener
import io.igrant.mobileagent.models.Notification
import io.igrant.mobileagent.models.credentialExchange.RawCredential
import io.igrant.mobileagent.models.walletSearch.Record
import io.igrant.mobileagent.utils.DateUtils
import io.igrant.mobileagent.utils.MessageTypes

class RequestListAdapter(
    private val mList: ArrayList<Record>,
    private val mListener: ConnectionMessageListener
) : RecyclerView.Adapter<RequestListAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivLogo: ImageView = itemView.findViewById(R.id.ivLogo) as ImageView
        var tvName: TextView = itemView.findViewById<View>(R.id.tvName) as TextView
        var tvType: TextView = itemView.findViewById<View>(R.id.tvType) as TextView
        var tvDate: TextView = itemView.findViewById(R.id.tvDate) as TextView
        var cvOffer: CardView = itemView.findViewById<View>(R.id.cvOffer) as CardView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.item_notification,
                    parent,
                    false
                )
        )
    }

    override fun getItemCount(): Int {
        return mList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val message =
            WalletManager.getGson.fromJson(mList[position].value, Notification::class.java)

        try {
            Glide
                .with(holder.ivLogo.context)
                .load(message.connection?.theirImageUrl ?: "")
                .centerCrop()
                .placeholder(R.drawable.images)
                .into(holder.ivLogo)
        } catch (e: Exception) {
        }

        if (message.type == MessageTypes.TYPE_REQUEST_PRESENTATION) {
            holder.tvName.text = (message.presentation?.presentationRequest?.name ?: "").toUpperCase()
        } else {
            val schema = WalletManager.getGson.fromJson(
                Base64.decode(
                    message.certificateOffer?.offersAttach?.get(0)?.data?.base64,
                    Base64.URL_SAFE
                )
                    .toString(charset("UTF-8")), RawCredential::class.java
            ).schemaId
            val lst = schema?.split(":")
            holder.tvName.text = (lst?.get(2) ?: "").toUpperCase()
        }

        holder.tvType.text =
            if (message.type == MessageTypes.TYPE_REQUEST_PRESENTATION) holder.tvType.context.resources.getString(
                R.string.txt_data_exchange
            ) else holder.tvType.context.resources.getString(R.string.txt_data_agreement)

        if (message.date != null && message.date != "")
            holder.tvDate.text = DateUtils.getRelativeTime(message.date ?: "")
        else
            holder.tvDate.text = "nil"

        holder.cvOffer.setOnClickListener {
            mListener.onConnectionMessageClick(
                mList[position],
                message.presentation?.presentationRequest?.name ?: ""
            )
        }
    }
}