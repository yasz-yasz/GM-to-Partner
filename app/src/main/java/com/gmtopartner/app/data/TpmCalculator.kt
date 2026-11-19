package com.gmtopartner.app.data

import java.text.NumberFormat
import java.util.Locale

data class TpmBand(
    val group: String,
    val rangeLabel: String,
    val economyMiles: Int,
    val businessMilesActual: Int,
    val businessMilesWebTypo: Int
)

object TpmCalculator {

    val BANDS: List<TpmBand> = listOf(
        TpmBand("A", "1 – 1.000", 15_000, 25_000, 25_000),
        TpmBand("B", "1.001 – 1.500", 17_500, 30_000, 30_000),
        TpmBand("C", "1.501 – 2.000", 22_500, 37_500, 37_500),
        TpmBand("D", "2.001 – 2.500", 27_500, 47_500, 77_500),
        TpmBand("E", "2.501 – 3.000", 32_500, 57_500, 57_500),
        TpmBand("F", "3.001 – 4.000", 37_500, 67_500, 67_500),
        TpmBand("G", "4.001 – 6.000", 42_500, 75_000, 75_000),
        TpmBand("H", "6.001 – 8.000", 52_500, 95_000, 95_000),
        TpmBand("I", "8.001 – 10.000", 57_500, 100_000, 100_000)
    )

    fun groupForDistance(distanceMiles: Int): String = when {
        distanceMiles <= 1000 -> "A"
        distanceMiles <= 1500 -> "B"
        distanceMiles <= 2000 -> "C"
        distanceMiles <= 2500 -> "D"
        distanceMiles <= 3000 -> "E"
        distanceMiles <= 4000 -> "F"
        distanceMiles <= 6000 -> "G"
        distanceMiles <= 8000 -> "H"
        else -> "I"
    }

    fun milesForGroup(
        group: String,
        cabin: CabinClass,
        useGroupDTypoRate: Boolean = false
    ): Int {
        val band = BANDS.firstOrNull { it.group == group } ?: BANDS.last()
        return when (cabin) {
            CabinClass.ECONOMY -> band.economyMiles
            CabinClass.BUSINESS -> if (useGroupDTypoRate) band.businessMilesWebTypo else band.businessMilesActual
        }
    }

    fun formatNumber(value: Int): String {
        return NumberFormat.getNumberInstance(Locale("id", "ID")).format(value)
    }
}
