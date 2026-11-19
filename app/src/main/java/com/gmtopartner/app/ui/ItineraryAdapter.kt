package com.gmtopartner.app.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.gmtopartner.app.R
import com.gmtopartner.app.data.CabinClass
import com.gmtopartner.app.data.ItineraryOption
import com.gmtopartner.app.data.TpmCalculator
import com.gmtopartner.app.databinding.ItemItineraryBinding
import com.gmtopartner.app.databinding.ItemSegmentRowBinding

class ItineraryAdapter(
    private val onCopyClick: (ItineraryOption) -> Unit
) : RecyclerView.Adapter<ItineraryAdapter.ViewHolder>() {

    private var items: List<ItineraryOption> = emptyList()
    private var cabin: CabinClass = CabinClass.BUSINESS
    private var isRoundTrip: Boolean = false
    private var cheapestMiles: Int = 0

    fun submitList(list: List<ItineraryOption>, cabin: CabinClass, isRoundTrip: Boolean) {
        this.items = list
        this.cabin = cabin
        this.isRoundTrip = isRoundTrip
        this.cheapestMiles = list.minOfOrNull { it.totalMilesOneWay } ?: 0
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemItineraryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class ViewHolder(private val b: ItemItineraryBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: ItineraryOption) {
            val ctx = b.root.context
            val multiplier = if (isRoundTrip) 2 else 1
            val displayedMiles = item.totalMilesOneWay * multiplier

            when {
                item.legCount == 1 -> {
                    b.tvBadgePrimary.text = "DIRECT"
                    b.tvBadgePrimary.setBackgroundResource(R.drawable.bg_badge_direct)
                    b.tvBadgePrimary.setTextColor(ContextCompat.getColor(ctx, R.color.badge_direct_text))
                }
                item.isMixPartner -> {
                    b.tvBadgePrimary.text = "MIX PARTNER"
                    b.tvBadgePrimary.setBackgroundResource(R.drawable.bg_badge_mix)
                    b.tvBadgePrimary.setTextColor(ContextCompat.getColor(ctx, R.color.badge_mix_text))
                }
                else -> {
                    b.tvBadgePrimary.text = "SINGLE PARTNER"
                    b.tvBadgePrimary.setBackgroundResource(R.drawable.bg_badge_single)
                    b.tvBadgePrimary.setTextColor(ContextCompat.getColor(ctx, R.color.badge_single_text))
                }
            }

            if (item.totalMilesOneWay == cheapestMiles && cheapestMiles > 0) {
                b.tvBadgeSecondary.visibility = View.VISIBLE
                b.tvBadgeSecondary.text = "TERHEMAT"
            } else {
                b.tvBadgeSecondary.visibility = View.GONE
            }

            b.tvBadgeFf.visibility = if (item.hasFifthFreedom) View.VISIBLE else View.GONE

            b.tvRoutePath.text = item.pathDisplay
            val airlinesText = item.uniqueAirlines.joinToString(" + ")
            val distText = TpmCalculator.formatNumber(item.totalDistanceMiles)
            val legText = if (item.legCount == 1) "Direct (1 Segmen)" else "${item.legCount} Segmen (${item.legCount - 1} Transit)"
            b.tvAirlinesInvolved.text = "$airlinesText • $legText • $distText mil"

            b.tvTotalMiles.text = TpmCalculator.formatNumber(displayedMiles)
            val tripLabel = if (isRoundTrip) "PP" else "OW"
            b.tvMilesLabel.text = "GarudaMiles (${cabin.label} $tripLabel)"

            b.containerSegments.removeAllViews()
            val inflater = LayoutInflater.from(ctx)
            for (seg in item.segments) {
                val segBinding = ItemSegmentRowBinding.inflate(inflater, b.containerSegments, false)
                segBinding.tvSegAirlineCode.text = seg.airlineCode
                val ffMarker = if (seg.isFifthFreedom) " ★5th" else ""
                segBinding.tvSegRoute.text = "${seg.fromCode} → ${seg.toCode} • ${seg.airlineName}$ffMarker"
                segBinding.tvSegMeta.text =
                    "${seg.fromCity} → ${seg.toCity} • ${TpmCalculator.formatNumber(seg.distanceMiles)} mil • Group ${seg.tpmGroup}"
                val segMiles = seg.milesRequired * multiplier
                segBinding.tvSegMiles.text = TpmCalculator.formatNumber(segMiles)
                b.containerSegments.addView(segBinding.root)
            }

            if (item.hasWarning) {
                b.tvItineraryWarning.visibility = View.VISIBLE
                b.tvItineraryWarning.text = item.warningText
            } else {
                b.tvItineraryWarning.visibility = View.GONE
            }

            b.btnCopyItinerary.setOnClickListener { onCopyClick(item) }
        }
    }
}
