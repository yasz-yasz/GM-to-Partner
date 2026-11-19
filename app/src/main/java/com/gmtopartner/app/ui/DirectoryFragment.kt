package com.gmtopartner.app.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.gmtopartner.app.MainActivity
import com.gmtopartner.app.data.RouteRepository
import com.gmtopartner.app.data.TpmCalculator
import com.gmtopartner.app.databinding.FragmentDirectoryBinding
import com.google.android.material.chip.Chip

class DirectoryFragment : Fragment() {

    private var _binding: FragmentDirectoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var repo: RouteRepository
    private lateinit var adapter: RouteRowAdapter

    private var selectedAirlineCode: String = "ALL"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDirectoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repo = RouteRepository.getInstance(requireContext())

        val totalRoutes = TpmCalculator.formatNumber(repo.routes.size)
        val totalAirports = TpmCalculator.formatNumber(repo.airports.size)
        val ffCount = repo.fifthFreedomRoutes.size
        binding.tvDirStats.text =
            "${repo.airlines.size} Maskapai Partner • $totalRoutes Rute Direct & 5th Freedom ($ffCount FF) • $totalAirports Bandara Global"

        adapter = RouteRowAdapter(repo) { route ->
            (activity as? MainActivity)?.openCalculatorWithRoute(route.from, route.to)
        }
        binding.rvDirRoutes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDirRoutes.adapter = adapter

        buildAirlineChips()
        applyFilter()

        binding.etSearchDir.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                applyFilter()
            }
        })
    }

    private fun buildAirlineChips() {
        val group = binding.chipGroupAirlines
        group.removeAllViews()

        val allChip = Chip(requireContext()).apply {
            id = View.generateViewId()
            text = "Semua (${repo.routes.size})"
            isCheckable = true
            isChecked = true
            setOnClickListener {
                selectedAirlineCode = "ALL"
                applyFilter()
            }
        }
        group.addView(allChip)

        val countsByAl = repo.routes.groupingBy { it.airlineCode }.eachCount()
        for (al in repo.airlines) {
            val count = countsByAl[al.code] ?: 0
            val chip = Chip(requireContext()).apply {
                id = View.generateViewId()
                text = "${al.code} • ${al.name} ($count)"
                isCheckable = true
                setOnClickListener {
                    selectedAirlineCode = al.code
                    applyFilter()
                }
            }
            group.addView(chip)
        }
    }

    private fun applyFilter() {
        val q = binding.etSearchDir.text?.toString()?.trim()?.lowercase().orEmpty()
        val baseList = if (selectedAirlineCode == "ALL") {
            repo.routes
        } else {
            repo.routes.filter { it.airlineCode == selectedAirlineCode }
        }

        val filtered = if (q.isEmpty()) {
            baseList
        } else {
            baseList.filter { r ->
                val al = repo.airlineMap[r.airlineCode]
                val a1 = repo.airportMap[r.from]
                val a2 = repo.airportMap[r.to]
                r.from.lowercase().contains(q) ||
                    r.to.lowercase().contains(q) ||
                    r.airlineCode.lowercase().contains(q) ||
                    (al?.name?.lowercase()?.contains(q) == true) ||
                    (a1?.searchKey()?.contains(q) == true) ||
                    (a2?.searchKey()?.contains(q) == true)
            }
        }

        val shown = filtered.take(250)
        if (selectedAirlineCode == "ALL") {
            binding.tvSelectedAirlineInfo.text = if (filtered.size > 250) {
                "Menampilkan 250 dari ${TpmCalculator.formatNumber(filtered.size)} rute. Ketik nama kota/kode IATA atau pilih maskapai untuk mempersempit."
            } else {
                "Menampilkan ${TpmCalculator.formatNumber(filtered.size)} rute (ketuk kartu untuk simulasi)."
            }
        } else {
            val al = repo.airlineMap[selectedAirlineCode]
            val hubs = al?.hubs?.joinToString(", ") ?: "-"
            val note = al?.notes ?: ""
            binding.tvSelectedAirlineInfo.text =
                "${al?.name} (${al?.alliance}) • Hub: $hubs • ${filtered.size} rute cocok.
$note"
        }

        adapter.submitList(shown)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
