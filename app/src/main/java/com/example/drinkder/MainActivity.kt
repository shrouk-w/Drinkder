package com.example.drinkder

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import com.example.drinkder.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var dashboardTapCount = 0
    private var lastDashboardTapAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        val homeIcon = findViewById<ImageView>(R.id.nav_home)
        val dashboardIcon = findViewById<ImageView>(R.id.nav_dashboard)
        val searchIcon = findViewById<ImageView>(R.id.nav_search)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val activeIcon = when (destination.id) {
                R.id.navigation_dashboard -> R.id.nav_dashboard
                R.id.navigation_search -> R.id.nav_search
                else -> R.id.nav_home
            }
            updateNavigationBar(activeIcon)
        }

        homeIcon.setOnClickListener {
            resetDashboardTapSequence()
            navController.navigate(R.id.navigation_home)
        }

        dashboardIcon.setOnClickListener {
            registerDashboardTap()
            navController.navigate(R.id.navigation_dashboard)
        }

        searchIcon.setOnClickListener {
            resetDashboardTapSequence()
            navController.navigate(R.id.navigation_search)
        }
    }

    private fun registerDashboardTap() {
        val now = SystemClock.elapsedRealtime()
        dashboardTapCount = if (now - lastDashboardTapAt <= EASTER_EGG_WINDOW_MS) {
            dashboardTapCount + 1
        } else {
            1
        }
        lastDashboardTapAt = now

        if (dashboardTapCount >= REQUIRED_DASHBOARD_TAPS) {
            resetDashboardTapSequence()
            startActivity(Intent(this, DoomActivity::class.java))
        }
    }

    private fun resetDashboardTapSequence() {
        dashboardTapCount = 0
        lastDashboardTapAt = 0L
    }

    private fun updateNavigationBar(activeId: Int) {
        val allIcons = listOf(
            findViewById<ImageView>(R.id.nav_home),
            findViewById<ImageView>(R.id.nav_dashboard),
            findViewById<ImageView>(R.id.nav_search)
        )

        allIcons.forEach { icon ->
            val isActive = icon.id == activeId
            val scale = if (isActive) 1.4f else 1.1f
            val color = if (isActive)
                ContextCompat.getColor(this, R.color.nav_active)
            else
                ContextCompat.getColor(this, R.color.nav_inactive)

            icon.animate().scaleX(scale).scaleY(scale).setDuration(200).start()
            icon.setColorFilter(color)
        }
    }

    companion object {
        private const val REQUIRED_DASHBOARD_TAPS = 5
        private const val EASTER_EGG_WINDOW_MS = 2_000L
    }
}
