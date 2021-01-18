package io.igrant.mobileagent.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import io.igrant.mobileagent.R
import io.igrant.mobileagent.listeners.LanguageClickListener
import io.igrant.mobileagent.listeners.LedgerNetworkClickListener
import io.igrant.mobileagent.models.Language
import io.igrant.mobileagent.models.ledger.LedgerItem

class LanguageAdapter(
    private val list: ArrayList<Language>,
    private val mListener: LanguageClickListener
) :
    RecyclerView.Adapter<LanguageAdapter.ViewHolder>() {
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
        holder.tvNetworkName.text = list[position].language
        holder.ivChecked.visibility =
            if (list[position].isChecked) View.VISIBLE else View.GONE
        holder.clItem.setOnClickListener {
            mListener.onLanguageClick(list[position]?.languageCode ?: "en")
        }
    }
}