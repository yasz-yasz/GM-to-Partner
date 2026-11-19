package com.gmtopartner.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.gmtopartner.app.data.CabinClass
import com.gmtopartner.app.data.RouteEdge
import com.gmtopartner.app.data.RouteRepository
import com.gmtopartner.app.data.TpmCalculator
import com.gmtopartner.app.databinding.ItemRouteRowBinding

class RouteRowAdapter(
    private val repo: RouteRepository,
    private val onRouteClick: (RouteEdge) -> Unit
) : RecyclerView.Adapter<RouteRowAdapter.ViewHolder>() {

    private var items: List<RouteEdge> = emptyList()

    fun submitList(list: List<RouteEdge>) {
        this.items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRouteRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class ViewHolder(private val b: ItemRouteRowBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(route: RouteEdge) {
            val airline = repo.airlineMap[route.airlineCode]
            val apFrom = repo.airportMap[route.from]
            val apTo = repo.airportMap[route.to]

            b.tvRowAirlineBadge.text = "${route.airlineCode} • ${airline?.name ?: route.airlineCode}"
            b.tvRowFfTag.visibility = if (route.isFifthFreedom) View.VISIBLE else View.GONE

            when {
                airline?.suspended == true -> {
                    b.tvRowWarnTag.visibility = View.VISIBLE
                    b.tvRowWarnTag.text = "SUSPENDED"
                }
                route.airlineCode == "SQ" && !route.isEligibleGm -> {
                    b.tvRowWarnTag.visibility = View.VISIBLE
                    b.tvRowWarnTag.text = "NON-CODESHARE"
                }
                else -> {
                    b.tvRowWarnTag.visibility = View.GONE
                }
            }

            b.tvRowGroup.text = "Group ${route.tpmGroup}"
            b.tvRowRouteCodes.text = "${route.from} ↔ ${route.to}  (${TpmCalculator.formatNumber(route.distanceMiles)} mil)"
            val cityFrom = apFrom?.let { "${it.city} (${it.country})" } ?: route.from
            val cityTo = apTo?.let { "${it.city} (${it.country})" } ?: route.to
            b.tvRowCities.text = "$cityFrom ↔ $cityTo"

            val ecoMiles = TpmCalculator.milesForGroup(route.tpmGroup, CabinClass.ECONOMY)
            val bizMiles = TpmCalculator.milesForGroup(route.tpmGroup, CabinClass.BUSINESS)
            b.tvRowEco.text = "Economy: ${TpmCalculator.formatNumber(ecoMiles)} miles"
            b.tvRowBiz.text = "Business: ${TpmCalculator.formatNumber(bizMiles)} miles"

            if (route.note.isNotBlank()) {
                b.tvRowNote.visibility = View.VISIBLE
                b.tvRowNote.text = route.note
            } else {
                b.tvRowNote.visibility = View.GONE
            }

            b.root.setOnClickListener { onRouteClick(route) }
        }
    }
}
