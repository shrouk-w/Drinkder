package com.example.drinkder

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.drinkder.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.navigation_home, R.id.navigation_dashboard)
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        val homeIcon = findViewById<ImageView>(R.id.nav_home)
        val dashboardIcon = findViewById<ImageView>(R.id.nav_dashboard)

        updateNavigationBar(R.id.nav_home)

        homeIcon.setOnClickListener {
            navController.navigate(R.id.navigation_home)
            updateNavigationBar(R.id.nav_home)
        }

        dashboardIcon.setOnClickListener {
            navController.navigate(R.id.navigation_dashboard)
            updateNavigationBar(R.id.nav_dashboard)
        }
    }

    private fun updateNavigationBar(activeId: Int) {
        val allIcons = listOf(
            findViewById<ImageView>(R.id.nav_home),
            findViewById<ImageView>(R.id.nav_dashboard),
        )

        allIcons.forEach { icon ->
            val isActive = icon.id == activeId
            val scale = if (isActive) 1.3f else 1.0f
            val color = if (isActive)
                ContextCompat.getColor(this, R.color.nav_active)
            else
                ContextCompat.getColor(this, R.color.nav_inactive)

            icon.animate().scaleX(scale).scaleY(scale).setDuration(200).start()
            icon.setColorFilter(color)
        }
    }
}
