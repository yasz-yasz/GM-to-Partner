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
import com.gmtopartner.app.databinding.FragmentFifthFreedomBinding

class FifthFreedomFragment : Fragment() {

    private var _binding: FragmentFifthFreedomBinding? = null
    private val binding get() = _binding!!

    private lateinit var repo: RouteRepository
    private lateinit var adapter: RouteRowAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFifthFreedomBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repo = RouteRepository.getInstance(requireContext())
        adapter = RouteRowAdapter(repo) { route ->
            (activity as? MainActivity)?.openCalculatorWithRoute(route.from, route.to)
        }
        binding.rvFfRoutes.layoutManager = LinearLayoutManager(requireContext())
        binding.rvFfRoutes.adapter = adapter

        filterRoutes("")

        binding.etSearchFf.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                filterRoutes(s?.toString().orEmpty())
            }
        })
    }

    private fun filterRoutes(query: String) {
        val q = query.trim().lowercase()
        val allFf = repo.fifthFreedomRoutes
        val filtered = if (q.isEmpty()) {
            allFf
        } else {
            allFf.filter { r ->
                val al = repo.airlineMap[r.airlineCode]
                val a1 = repo.airportMap[r.from]
                val a2 = repo.airportMap[r.to]
                r.from.lowercase().contains(q) ||
                    r.to.lowercase().contains(q) ||
                    r.airlineCode.lowercase().contains(q) ||
                    (al?.name?.lowercase()?.contains(q) == true) ||
                    (a1?.searchKey()?.contains(q) == true) ||
                    (a2?.searchKey()?.contains(q) == true) ||
                    r.note.lowercase().contains(q)
            }
        }
        binding.tvFfCount.text =
            "Menampilkan ${filtered.size} dari ${allFf.size} rute Fifth Freedom (ketuk kartu untuk simulasi)"
        adapter.submitList(filtered)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
