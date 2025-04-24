package com.example.drinkder

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
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

        homeIcon.setOnClickListener {
            navController.navigate(R.id.navigation_home)
        }

        dashboardIcon.setOnClickListener {
            navController.navigate(R.id.navigation_dashboard)
        }
    }
}
