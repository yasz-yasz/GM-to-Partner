package com.gmtopartner.app.ui

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TableRow
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.gmtopartner.app.R
import com.gmtopartner.app.data.TpmCalculator
import com.gmtopartner.app.databinding.FragmentTpmChartBinding

class TpmChartFragment : Fragment() {

    private var _binding: FragmentTpmChartBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTpmChartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        populateTable()

        binding.tvTpmRules.text = """
            • Berlaku untuk seluruh maskapai SkyTeam & Non-SkyTeam Partner GarudaMiles.
            • Tarif dihitung terpisah PER SEGMEN berdasarkan jarak tempuh TPM (Ticketed Point Mileage) sekali jalan (One-Way). Jika transit 2 segmen (misalnya CGK→JED + JED→IST), maka miles dijumlahkan dari masing-masing segmen.
            • Group D Business: Website Garuda Indonesia mencantumkan angka 77.500 (typo). Tarif aktual di sistem penukaran umumnya adalah 47.500 miles. Anda dapat mengganti preferensi ini di menu Pengaturan Lanjutan pada Kalkulator.
            • Hanya berlaku untuk Economy & Business Class (tidak berlaku First Class / Upgrade Award).
            • Singapore Airlines: Penukaran resmi GarudaMiles saat ini dibatasi pada rute codeshare terpilih (SIN ↔ CGK, DPS, SUB, KNO, BOM, LHR).
            • Aeroflot: Masih tercantum di website Garuda, namun keanggotaan SkyTeam sedang ditangguhkan (suspended).
            • Penukaran award ticket partner dilakukan melalui Garuda Indonesia Sales Office (GASO) atau kanal resmi GarudaMiles.
        """.trimIndent()
    }

    private fun populateTable() {
        val table = binding.tableTpm
        table.removeAllViews()

        val ctx = requireContext()
        val headerRow = TableRow(ctx).apply {
            setBackgroundColor(ContextCompat.getColor(ctx, R.color.navy_primary))
            setPadding(8, 14, 8, 14)
        }
        listOf("Grup", "Jarak (Mil)", "Economy", "Business").forEach { title ->
            headerRow.addView(makeCell(title, bold = true, colorRes = android.R.color.white))
        }
        table.addView(headerRow)

        TpmCalculator.BANDS.forEachIndexed { idx, band ->
            val row = TableRow(ctx).apply {
                val bg = if (idx % 2 == 0) R.color.card_white else R.color.bg_surface
                setBackgroundColor(ContextCompat.getColor(ctx, bg))
                setPadding(8, 12, 8, 12)
            }
            val bizLabel = if (band.group == "D") {
                "${TpmCalculator.formatNumber(band.businessMilesActual)}*"
            } else {
                TpmCalculator.formatNumber(band.businessMilesActual)
            }
            row.addView(makeCell("Group ${band.group}", bold = true, colorRes = R.color.navy_primary))
            row.addView(makeCell(band.rangeLabel, bold = false, colorRes = R.color.text_primary))
            row.addView(makeCell(TpmCalculator.formatNumber(band.economyMiles), bold = false, colorRes = R.color.text_primary))
            row.addView(makeCell(bizLabel, bold = true, colorRes = R.color.teal_accent))
            table.addView(row)
        }
    }

    private fun makeCell(text: String, bold: Boolean, colorRes: Int): TextView {
        return TextView(requireContext()).apply {
            this.text = text
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(ContextCompat.getColor(requireContext(), colorRes))
            if (bold) setTypeface(null, Typeface.BOLD)
            setPadding(4, 4, 4, 4)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
