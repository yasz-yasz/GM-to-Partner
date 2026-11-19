package com.gmtopartner.app.ui

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter
import com.gmtopartner.app.data.Airport

class AirportAutoCompleteAdapter(
    context: Context,
    private val allAirports: List<Airport>
) : ArrayAdapter<String>(context, android.R.layout.simple_dropdown_item_1line) {

    private var filteredLabels: List<String> = emptyList()

    override fun getCount(): Int = filteredLabels.size

    override fun getItem(position: Int): String = filteredLabels[position]

    override fun getFilter(): Filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val results = FilterResults()
            val query = constraint?.toString()?.trim()?.lowercase().orEmpty()
            if (query.isEmpty()) {
                results.values = emptyList<String>()
                results.count = 0
                return results
            }

            val exactCode = allAirports.filter { it.code.lowercase() == query }
            val startsCode = allAirports.filter { it.code.lowercase().startsWith(query) && it.code.lowercase() != query }
            val startsCity = allAirports.filter {
                !it.code.lowercase().startsWith(query) && it.city.lowercase().startsWith(query)
            }
            val containsAny = allAirports.filter {
                !it.code.lowercase().startsWith(query) &&
                    !it.city.lowercase().startsWith(query) &&
                    it.searchKey().contains(query)
            }

            val combined = (exactCode + startsCode + startsCity + containsAny)
                .take(15)
                .map { it.displayLabel() }

            results.values = combined
            results.count = combined.size
            return results
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
            filteredLabels = (results?.values as? List<String>) ?: emptyList()
            notifyDataSetChanged()
        }
    }
}
