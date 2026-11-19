package com.gmtopartner.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.gmtopartner.app.data.RouteRepository
import com.gmtopartner.app.data.TpmCalculator
import com.gmtopartner.app.databinding.ActivityMainBinding
import com.gmtopartner.app.ui.CalculatorFragment
import com.gmtopartner.app.ui.DirectoryFragment
import com.gmtopartner.app.ui.FifthFreedomFragment
import com.gmtopartner.app.ui.TpmChartFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val calculatorFragment = CalculatorFragment()
    private val fifthFreedomFragment = FifthFreedomFragment()
    private val directoryFragment = DirectoryFragment()
    private val tpmChartFragment = TpmChartFragment()
    private var activeFragment: Fragment = calculatorFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val repo = RouteRepository.getInstance(this)
        binding.tvToolbarSubtitle.text =
            "${repo.airlines.size} Maskapai Partner • ${TpmCalculator.formatNumber(repo.routes.size)} Rute Direct & 5th Freedom"

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, tpmChartFragment, "tpm").hide(tpmChartFragment)
                .add(R.id.fragment_container, directoryFragment, "dir").hide(directoryFragment)
                .add(R.id.fragment_container, fifthFreedomFragment, "ff").hide(fifthFreedomFragment)
                .add(R.id.fragment_container, calculatorFragment, "calc")
                .commit()
        }

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_calculator -> switchFragment(calculatorFragment)
                R.id.nav_fifth_freedom -> switchFragment(fifthFreedomFragment)
                R.id.nav_directory -> switchFragment(directoryFragment)
                R.id.nav_tpm -> switchFragment(tpmChartFragment)
                else -> false
            }
        }
    }

    private fun switchFragment(target: Fragment): Boolean {
        if (target == activeFragment) return true
        supportFragmentManager.beginTransaction()
            .hide(activeFragment)
            .show(target)
            .commit()
        activeFragment = target
        return true
    }

    fun openCalculatorWithRoute(originCode: String, destCode: String) {
        binding.bottomNavigation.selectedItemId = R.id.nav_calculator
        switchFragment(calculatorFragment)
        calculatorFragment.searchExternalRoute(originCode, destCode)
    }
}
