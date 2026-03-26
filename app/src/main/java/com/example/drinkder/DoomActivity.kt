package com.example.drinkder

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.drinkder.data.FavoritesStore
import com.example.drinkder.databinding.ActivityDoomBinding
import com.example.drinkder.network.RetrofitInstance
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DoomActivity : AppCompatActivity() {

    private data class EnemyDrink(
        val name: String,
        val imageUrl: String
    )

    private lateinit var binding: ActivityDoomBinding
    private var pageReady = false
    private var savedDrinks: List<EnemyDrink> = DEFAULT_ENEMIES

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDoomBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.closeDoom.setOnClickListener { finish() }

        binding.doomWebView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            loadWithOverviewMode = true
            useWideViewPort = true
            allowFileAccess = true
            allowContentAccess = true
        }

        binding.doomWebView.webChromeClient = android.webkit.WebChromeClient()
        binding.doomWebView.webViewClient = object : android.webkit.WebViewClient() {
            override fun onPageFinished(view: android.webkit.WebView, url: String?) {
                super.onPageFinished(view, url)
                pageReady = true
                binding.doomLoading.visibility = android.view.View.GONE
                pushSavedDrinksToGame()
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })

        binding.doomWebView.loadUrl(DOOM_URL)
        loadSavedDrinks()
    }

    override fun onDestroy() {
        binding.doomWebView.destroy()
        super.onDestroy()
    }

    private fun loadSavedDrinks() {
        lifecycleScope.launch {
            savedDrinks = withContext(Dispatchers.IO) {
                val ids = FavoritesStore(applicationContext).getIdsOnce().toList()
                if (ids.isEmpty()) {
                    DEFAULT_ENEMIES
                } else {
                    ids.map { id ->
                        async {
                            runCatching {
                                RetrofitInstance.api.getDrinkById(id).drinks?.firstOrNull()?.let { drink ->
                                    EnemyDrink(
                                        name = drink.name,
                                        imageUrl = drink.imageUrl
                                    )
                                }
                            }.getOrNull()
                        }
                    }.mapNotNull { it.await() }
                        .ifEmpty { DEFAULT_ENEMIES }
                }
            }
            pushSavedDrinksToGame()
        }
    }

    private fun pushSavedDrinksToGame() {
        if (!pageReady) return
        val payload = Gson().toJson(savedDrinks.take(8))
        binding.doomWebView.evaluateJavascript(
            "window.setSavedDrinks($payload);",
            null
        )
    }

    companion object {
        private const val DOOM_URL = "file:///android_asset/doom/index.html"
        private val DEFAULT_ENEMIES = listOf(
            EnemyDrink("Negroni", "https://www.thecocktaildb.com/images/media/drink/qgdu971561574065.jpg"),
            EnemyDrink("Margarita", "https://www.thecocktaildb.com/images/media/drink/5noda61589575158.jpg"),
            EnemyDrink("Old Fashioned", "https://www.thecocktaildb.com/images/media/drink/vrwquq1478252802.jpg"),
            EnemyDrink("Mojito", "https://www.thecocktaildb.com/images/media/drink/metwgh1606770327.jpg")
        )
    }
}
