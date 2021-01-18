package io.igrant.mobileagent.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import io.igrant.mobileagent.R
import io.igrant.mobileagent.listeners.LedgerNetworkClickListener
import io.igrant.mobileagent.models.ledger.LedgerItem

class LedgerNetworkAdapter(
    private val list: ArrayList<LedgerItem>,
    private var selectedNetwork: Int,
    private val mListener: LedgerNetworkClickListener
) :
    RecyclerView.Adapter<LedgerNetworkAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var vDivider: View = itemView.findViewById<View>(R.id.vDivider) as View
        val tvNetworkName: TextView = itemView.findViewById(R.id.tvNetworkName) as TextView
        val ivChecked: ImageView = itemView.findViewById(R.id.ivChecked)
        val clItem: ConstraintLayout = itemView.findViewById(R.id.clItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_ledger_network, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.vDivider.visibility = if (position == list.size - 1) View.GONE else View.VISIBLE
        holder.tvNetworkName.text = list[position].name
        holder.ivChecked.visibility =
            if (list[position].type == selectedNetwork) View.VISIBLE else View.GONE
        holder.clItem.setOnClickListener {
            mListener.onNetworkClick(list[position]?.type ?: 0)
        }
    }

    fun setType(selectedNetwork: Int) {
        this.selectedNetwork = selectedNetwork
        notifyDataSetChanged()
    }
}