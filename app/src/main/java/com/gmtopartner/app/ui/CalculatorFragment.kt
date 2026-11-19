package com.gmtopartner.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.gmtopartner.app.R
import com.gmtopartner.app.data.CabinClass
import com.gmtopartner.app.data.ItineraryOption
import com.gmtopartner.app.data.RoutePlannerEngine
import com.gmtopartner.app.data.RouteRepository
import com.gmtopartner.app.data.SearchMode
import com.gmtopartner.app.data.SearchParams
import com.gmtopartner.app.data.TpmCalculator
import com.gmtopartner.app.databinding.FragmentCalculatorBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CalculatorFragment : Fragment() {

    private var _binding: FragmentCalculatorBinding? = null
    private val binding get() = _binding!!

    private lateinit var repo: RouteRepository
    private lateinit var engine: RoutePlannerEngine
    private lateinit var adapter: ItineraryAdapter

    private var advancedExpanded = false
    private var lastCabin = CabinClass.BUSINESS
    private var lastRoundTrip = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalculatorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repo = RouteRepository.getInstance(requireContext())
        engine = RoutePlannerEngine(repo)

        val autoAdapter = AirportAutoCompleteAdapter(requireContext(), repo.airports)
        binding.etOrigin.setAdapter(autoAdapter)
        binding.etDestination.setAdapter(AirportAutoCompleteAdapter(requireContext(), repo.airports))

        adapter = ItineraryAdapter { option -> copyItinerary(option) }
        binding.rvResults.layoutManager = LinearLayoutManager(requireContext())
        binding.rvResults.adapter = adapter

        binding.etOrigin.setText(repo.airportMap["CGK"]?.displayLabel() ?: "CGK")
        binding.etDestination.setText(repo.airportMap["IST"]?.displayLabel() ?: "IST")

        setupListeners()
        runSearch()
    }

    fun searchExternalRoute(originCode: String, destCode: String) {
        if (_binding == null) return
        binding.etOrigin.setText(repo.airportMap[originCode]?.displayLabel() ?: originCode)
        binding.etDestination.setText(repo.airportMap[destCode]?.displayLabel() ?: destCode)
        runSearch()
    }

    private fun setupListeners() {
        binding.btnSwap.setOnClickListener {
            val orig = binding.etOrigin.text.toString()
            val dest = binding.etDestination.text.toString()
            binding.etOrigin.setText(dest)
            binding.etDestination.setText(orig)
        }

        binding.chipQuickCgkIst.setOnClickListener { searchExternalRoute("CGK", "IST") }
        binding.chipQuickCgkCdg.setOnClickListener { searchExternalRoute("CGK", "CDG") }
        binding.chipQuickCgkJfk.setOnClickListener { searchExternalRoute("CGK", "JFK") }
        binding.chipQuickCgkHnl.setOnClickListener { searchExternalRoute("CGK", "HNL") }
        binding.chipQuickDpsAkl.setOnClickListener { searchExternalRoute("DPS", "AKL") }
        binding.chipQuickCgkLhr.setOnClickListener { searchExternalRoute("CGK", "LHR") }

        binding.tvToggleAdvanced.setOnClickListener {
            advancedExpanded = !advancedExpanded
            binding.layoutAdvancedOptions.visibility = if (advancedExpanded) View.VISIBLE else View.GONE
            binding.tvToggleAdvanced.text = if (advancedExpanded) {
                "▾ Pengaturan Lanjutan (Group D, SQ, Aeroflot)"
            } else {
                "▸ Pengaturan Lanjutan (Group D, SQ, Aeroflot)"
            }
        }

        binding.etDestination.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                runSearch()
                true
            } else {
                false
            }
        }

        binding.btnCalculate.setOnClickListener { runSearch() }
    }

    private fun runSearch() {
        val originCode = repo.resolveAirportCode(binding.etOrigin.text.toString())
        val destCode = repo.resolveAirportCode(binding.etDestination.text.toString())

        if (originCode == null) {
            Toast.makeText(requireContext(), "Bandara asal tidak ditemukan. Gunakan kode IATA (cth: CGK) atau nama kota.", Toast.LENGTH_SHORT).show()
            return
        }
        if (destCode == null) {
            Toast.makeText(requireContext(), "Bandara tujuan tidak ditemukan. Gunakan kode IATA (cth: IST) atau nama kota.", Toast.LENGTH_SHORT).show()
            return
        }
        if (originCode == destCode) {
            Toast.makeText(requireContext(), "Bandara asal dan tujuan tidak boleh sama.", Toast.LENGTH_SHORT).show()
            return
        }

        val cabin = if (binding.toggleCabin.checkedButtonId == R.id.btn_cabin_economy) {
            CabinClass.ECONOMY
        } else {
            CabinClass.BUSINESS
        }
        val isRoundTrip = binding.switchRoundTrip.isChecked
        val maxLegs = when (binding.chipGroupMaxLegs.checkedChipId) {
            R.id.chip_legs_1 -> 1
            R.id.chip_legs_2 -> 2
            else -> 3
        }
        val mode = when (binding.chipGroupMode.checkedChipId) {
            R.id.chip_mode_single -> SearchMode.SINGLE_PARTNER
            R.id.chip_mode_mix -> SearchMode.MIX_PARTNER
            else -> SearchMode.ALL
        }

        val params = SearchParams(
            originCode = originCode,
            destinationCode = destCode,
            cabin = cabin,
            isRoundTrip = isRoundTrip,
            maxLegs = maxLegs,
            mode = mode,
            useGroupDTypoRate = binding.switchGroupDTypo.isChecked,
            includeSqNonCodeshare = binding.switchIncludeSqAll.isChecked,
            includeSuspendedAeroflot = binding.switchIncludeAeroflot.isChecked
        )

        lastCabin = cabin
        lastRoundTrip = isRoundTrip

        binding.progressCalculating.visibility = View.VISIBLE
        binding.tvEmptyState.visibility = View.GONE
        binding.layoutResultHeader.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {
            val results = withContext(Dispatchers.Default) {
                engine.search(params, maxResults = 60)
            }
            if (_binding == null) return@launch
            binding.progressCalculating.visibility = View.GONE

            if (results.isEmpty()) {
                adapter.submitList(emptyList(), cabin, isRoundTrip)
                binding.layoutResultHeader.visibility = View.GONE
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.tvEmptyState.text =
                    "Tidak ditemukan rute partner dari $originCode ke $destCode dengan filter saat ini. Coba naikkan Maks Segmen (hingga 3 Leg) atau pilih Mode Semua (Single & Mix)."
            } else {
                val multiplier = if (isRoundTrip) 2 else 1
                val minMiles = results.first().totalMilesOneWay * multiplier
                val singleCount = results.count { !it.isMixPartner }
                val mixCount = results.count { it.isMixPartner }
                val tripLabel = if (isRoundTrip) "PP (×2)" else "One-Way"

                binding.layoutResultHeader.visibility = View.VISIBLE
                binding.tvResultSummary.text =
                    "$originCode → $destCode • Mulai ${TpmCalculator.formatNumber(minMiles)} GarudaMiles (${cabin.label} $tripLabel)"
                binding.tvResultSubsummary.text =
                    "Menampilkan ${results.size} rekomendasi terbaik ($singleCount Single Partner, $mixCount Mix Partner)"

                adapter.submitList(results, cabin, isRoundTrip)
            }
        }
    }

    private fun copyItinerary(option: ItineraryOption) {
        val multiplier = if (lastRoundTrip) 2 else 1
        val tripLabel = if (lastRoundTrip) "PP (Round-Trip)" else "One-Way"
        val sb = StringBuilder()
        sb.appendLine("GM to Partner — Rekomendasi Redeem GarudaMiles")
        sb.appendLine("Rute: ${option.pathDisplay}")
        sb.appendLine("Kabin: ${lastCabin.label} ($tripLabel)")
        sb.appendLine("Total: ${TpmCalculator.formatNumber(option.totalMilesOneWay * multiplier)} GarudaMiles (${TpmCalculator.formatNumber(option.totalDistanceMiles)} mil)")
        sb.appendLine("Rincian Segmen:")
        option.segments.forEachIndexed { idx, seg ->
            val segMiles = TpmCalculator.formatNumber(seg.milesRequired * multiplier)
            sb.appendLine("  ${idx + 1}. [${seg.airlineCode}] ${seg.airlineName}: ${seg.fromCode}→${seg.toCode} (${TpmCalculator.formatNumber(seg.distanceMiles)} mi, Group ${seg.tpmGroup}) = $segMiles miles")
        }
        val cm = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("GM to Partner", sb.toString()))
        Toast.makeText(requireContext(), "Ringkasan rute disalin ke clipboard", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
