package com.gmtopartner.app.data

enum class CabinClass(val label: String) {
    ECONOMY("Economy"),
    BUSINESS("Business")
}

enum class SearchMode {
    ALL,
    SINGLE_PARTNER,
    MIX_PARTNER
}

data class Airport(
    val code: String,
    val name: String,
    val city: String,
    val country: String,
    val region: String,
    val lat: Double,
    val lon: Double
) {
    fun displayLabel(): String = "$code — $city ($country)"
    fun searchKey(): String = "$code $city $country $name $region".lowercase()
}

data class Airline(
    val code: String,
    val name: String,
    val alliance: String,
    val country: String,
    val hubs: List<String>,
    val suspended: Boolean,
    val notes: String
)

data class RouteEdge(
    val airlineCode: String,
    val from: String,
    val to: String,
    val distanceMiles: Int,
    val tpmGroup: String,
    val isFifthFreedom: Boolean,
    val isEligibleGm: Boolean,
    val note: String
)

data class ItinerarySegment(
    val airlineCode: String,
    val airlineName: String,
    val fromCode: String,
    val toCode: String,
    val fromCity: String,
    val toCity: String,
    val distanceMiles: Int,
    val tpmGroup: String,
    val milesRequired: Int,
    val isFifthFreedom: Boolean,
    val isEligibleGm: Boolean,
    val isSuspended: Boolean,
    val note: String
)

data class ItineraryOption(
    val segments: List<ItinerarySegment>,
    val totalMilesOneWay: Int,
    val totalDistanceMiles: Int,
    val isMixPartner: Boolean,
    val hasFifthFreedom: Boolean,
    val hasWarning: Boolean,
    val warningText: String
) {
    val legCount: Int get() = segments.size
    val uniqueAirlines: List<String> get() = segments.map { it.airlineName }.distinct()
    val pathDisplay: String
        get() = buildString {
            if (segments.isNotEmpty()) {
                append(segments.first().fromCode)
                for (seg in segments) {
                    append(" → ")
                    append(seg.toCode)
                }
            }
        }
}

data class SearchParams(
    val originCode: String,
    val destinationCode: String,
    val cabin: CabinClass,
    val isRoundTrip: Boolean,
    val maxLegs: Int,
    val mode: SearchMode,
    val useGroupDTypoRate: Boolean,
    val includeSqNonCodeshare: Boolean,
    val includeSuspendedAeroflot: Boolean
)
