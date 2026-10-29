package com.example.drinkder

import SwipeViewModel
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.widget.ImageView
import android.widget.PopupWindow
import android.view.View
import android.view.ViewGroup
import android.view.Gravity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavOptions
import androidx.navigation.findNavController
import com.example.drinkder.databinding.ActivityMainBinding
import com.example.drinkder.databinding.PopupDiscoverySourceBinding
import com.example.drinkder.model.DiscoverySource

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var dashboardTapCount = 0
    private var lastDashboardTapAt = 0L
    private val swipeViewModel: SwipeViewModel by viewModels()
    private var deckPopup: PopupWindow? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyBackgroundPreview(intent)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val keyboard = insets.getInsets(WindowInsetsCompat.Type.ime())
            view.setPadding(bars.left, bars.top, bars.right, maxOf(bars.bottom, keyboard.bottom))
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)

        val navController = findNavController(R.id.nav_host_fragment_activity_main)

        val homeIcon = findViewById<ImageView>(R.id.nav_home)
        val dashboardIcon = findViewById<ImageView>(R.id.nav_dashboard)
        val searchIcon = findViewById<ImageView>(R.id.nav_search)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val host = findViewById<android.view.View>(R.id.nav_host_fragment_activity_main)
            (host as? ViewGroup)?.apply {
                clipChildren = false
                clipToPadding = false
            }
            val isCollection = destination.id == R.id.navigation_dashboard || destination.id == R.id.navigation_search
            val bubble = findViewById<android.view.View>(R.id.navigation_bubble)
            bubble.translationZ = 16 * resources.displayMetrics.density
            bubble.setBackgroundResource(if (isCollection) R.drawable.toolbar_collection_background else R.drawable.toolbar_background)
            val activeIcon = when (destination.id) {
                R.id.navigation_dashboard -> R.id.nav_dashboard
                R.id.navigation_search -> R.id.nav_search
                else -> R.id.nav_home
            }
            updateNavigationBar(activeIcon)
        }

        homeIcon.setOnClickListener {
            resetDashboardTapSequence()
            navigateTo(R.id.navigation_home)
        }
        homeIcon.contentDescription = getString(R.string.discover_hold_hint)
        homeIcon.setOnLongClickListener {
            resetDashboardTapSequence()
            showDeckMenu(homeIcon)
            true
        }
        ViewCompat.addAccessibilityAction(homeIcon, getString(R.string.choose_deck)) { _, _ ->
            showDeckMenu(homeIcon)
            true
        }

        dashboardIcon.setOnClickListener {
            registerDashboardTap()
            navigateTo(R.id.navigation_dashboard)
        }

        searchIcon.setOnClickListener {
            resetDashboardTapSequence()
            navigateTo(R.id.navigation_search)
        }
    }

    private fun showDeckMenu(anchor: View) {
        deckPopup?.dismiss()
        val menu = PopupDiscoverySourceBinding.inflate(layoutInflater)
        val density = resources.displayMetrics.density
        val width = minOf((264 * density).toInt(), binding.root.width - (32 * density).toInt())
        menu.root.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val popup = PopupWindow(menu.root, width, ViewGroup.LayoutParams.WRAP_CONTENT, true).apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            isOutsideTouchable = true
            elevation = 16 * density
        }
        deckPopup = popup
        ViewCompat.setAccessibilityPaneTitle(menu.root, getString(R.string.choose_deck))
        listOf(
            menu.deckRandom to DiscoverySource.RANDOM,
            menu.deckPopular to DiscoverySource.POPULAR,
            menu.deckNew to DiscoverySource.NEW
        ).forEach { (button, source) ->
            button.isSelected = swipeViewModel.source.value == source
            button.setOnClickListener {
                popup.dismiss()
                swipeViewModel.selectSource(source)
                navigateTo(R.id.navigation_home)
            }
        }
        popup.setOnDismissListener { if (deckPopup === popup) deckPopup = null }
        val bubble = findViewById<View>(R.id.navigation_bubble)
        val location = IntArray(2)
        bubble.getLocationOnScreen(location)
        val x = (location[0] + bubble.width / 2 - width / 2).coerceAtLeast((16 * density).toInt())
        val y = (location[1] - menu.root.measuredHeight - (12 * density).toInt()).coerceAtLeast(0)
        popup.showAtLocation(binding.root, Gravity.TOP or Gravity.START, x, y)
    }

    override fun onDestroy() {
        deckPopup?.dismiss()
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        applyBackgroundPreview(intent)
    }

    // Debug-only previews let us compare backgrounds on the same screen and card.
    private fun applyBackgroundPreview(intent: Intent) {
        if (!BuildConfig.DEBUG) return
        val background = when (intent.getStringExtra("background_preview")) {
            "gradient" -> R.drawable.background_signature
            "glass" -> R.drawable.background_rose_glass
            "metal" -> R.drawable.background_pearl_metal
            "plum" -> R.drawable.background_smoked_plum
            else -> R.drawable.gradient
        }
        binding.root.setBackgroundResource(background)
    }

    private fun navigateTo(destination: Int) {
        val controller = findNavController(R.id.nav_host_fragment_activity_main)
        if (controller.currentDestination?.id == destination) return
        val options = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setRestoreState(true)
            .setPopUpTo(controller.graph.startDestinationId, false, true)
            .build()
        controller.navigate(destination, null, options)
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
            val color = if (isActive)
                ContextCompat.getColor(this, R.color.nav_active)
            else
                ContextCompat.getColor(this, R.color.nav_inactive)

            icon.isSelected = isActive
            icon.setColorFilter(color)
        }
    }

    companion object {
        private const val REQUIRED_DASHBOARD_TAPS = 5
        private const val EASTER_EGG_WINDOW_MS = 2_000L
    }
}
