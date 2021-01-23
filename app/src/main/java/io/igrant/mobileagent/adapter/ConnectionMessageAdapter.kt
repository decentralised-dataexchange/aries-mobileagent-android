package io.igrant.mobileagent.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.igrant.mobileagent.R
import io.igrant.mobileagent.listeners.ConnectionMessageListener
import io.igrant.mobileagent.models.connection.Certificate

class ConnectionMessageAdapter(
    private val mList: ArrayList<Certificate>,
    private val mListener: ConnectionMessageListener
) :
    RecyclerView.Adapter<ConnectionMessageAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvName: TextView = itemView.findViewById<View>(R.id.tvName) as TextView
        var cvOffer: ConstraintLayout = itemView.findViewById<View>(R.id.cvOffer) as ConstraintLayout
        var ivAdd: ImageView = itemView.findViewById<View>(R.id.ivAdd) as ImageView
        var rvAttributes: RecyclerView = itemView.findViewById<View>(R.id.rvAttributes) as RecyclerView
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

        holder.ivAdd.visibility = if (mList[position].record != null) View.VISIBLE else View.GONE

        holder.cvOffer.setOnClickListener {
            if (mList[position].record != null)
                mListener.onConnectionMessageClick(mList[position].record!!,mList[position].schemaName?:"")
        }

        var adapter = RequestAttributeAdapter(
            mList[position].attributeList
        )
        holder.rvAttributes.layoutManager = LinearLayoutManager(holder.rvAttributes.context)
        holder.rvAttributes.adapter = adapter
    }
}