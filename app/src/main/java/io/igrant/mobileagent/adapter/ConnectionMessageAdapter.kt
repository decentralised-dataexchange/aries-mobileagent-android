package io.igrant.mobileagent.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import io.igrant.mobileagent.R
import io.igrant.mobileagent.listeners.ConnectionMessageListener
import io.igrant.mobileagent.models.connection.Certificate

class ConnectionMessageAdapter(
    val mList: ArrayList<Certificate>,
    val mListener: ConnectionMessageListener
) :
    RecyclerView.Adapter<ConnectionMessageAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvName: TextView = itemView.findViewById<View>(R.id.tvName) as TextView
        var tvVersion: TextView = itemView.findViewById<View>(R.id.tvVersion) as TextView
        var cvOffer: CardView = itemView.findViewById<View>(R.id.cvOffer) as CardView
        var ivAdd: ImageView = itemView.findViewById<View>(R.id.ivAdd) as ImageView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.item_connection_messaages,
                    parent,
                    false
                )
        )
    }

    override fun getItemCount(): Int {
        return mList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tvName.text = mList[position].schemaName

        holder.tvVersion.text = holder.tvVersion.context.resources.getString(R.string.txt_version_detail,mList[position].schemaVersion)

        holder.ivAdd.visibility = if (mList[position].record != null) View.VISIBLE else View.GONE

        holder.cvOffer.setOnClickListener {
            if (mList[position].record != null)
                mListener.onConnectionMessageClick(mList[position].record!!,mList[position].schemaName?:"")
        }
    }
}