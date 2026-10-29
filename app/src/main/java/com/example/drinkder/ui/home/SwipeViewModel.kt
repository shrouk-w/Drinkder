import android.app.Application
import androidx.lifecycle.*
import coil.Coil
import coil.request.ImageRequest
import com.example.drinkder.data.FavoritesStore
import com.example.drinkder.model.Drink
import com.example.drinkder.network.RetrofitInstance
import com.example.drinkder.network.DiscoveryRepository
import com.example.drinkder.model.DiscoverySource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SwipeViewModel(application: Application, savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {
    private val _drinks = savedStateHandle.getLiveData<List<Drink>>("drinks", emptyList())
    val drinks: LiveData<List<Drink>> = _drinks

    private val _favorites = savedStateHandle.getLiveData<List<Drink>>("favorites", emptyList())
    val favorites: LiveData<List<Drink>> = _favorites

    private val favoritesStore = FavoritesStore(getApplication<Application>())
    private val favoriteIds = mutableSetOf<String>()
    val isLoading = MutableLiveData(false)
    val error = MutableLiveData<String?>(null)

    private val _source = savedStateHandle.getLiveData("discovery_source", DiscoverySource.RANDOM)
    val source: LiveData<DiscoverySource> = _source
    private val discovery by lazy { DiscoveryRepository(RetrofitInstance.discoveryApi) }
    private var deckJob: Job? = null
    private var deckGeneration = 0L

    fun selectSource(selected: DiscoverySource) {
        if (_source.value == selected) return
        deckGeneration++
        deckJob?.cancel()
        _source.value = selected
        _drinks.value = emptyList()
        isLoading.value = false
        fetchDrink()
    }

    fun fetchDrink() {
        if (deckJob?.isActive == true || _drinks.value.orEmpty().size >= 4) return
        val selected = _source.value ?: DiscoverySource.RANDOM
        val generation = ++deckGeneration
        isLoading.value = true
        error.value = null
        deckJob = viewModelScope.launch {
            try {
                val batch = discovery.load(selected)
                ensureActive()
                if (generation != deckGeneration) return@launch
                val existing = _drinks.value.orEmpty()
                val ids = existing.mapTo(mutableSetOf()) { it.id }
                val next = batch.filter { ids.add(it.id) }
                _drinks.value = existing + next
                next.take(4).forEach { drink ->
                    Coil.imageLoader(getApplication()).enqueue(
                        ImageRequest.Builder(getApplication<Application>()).data(drink.imageUrl).build()
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                if (generation == deckGeneration) {
                    error.value = if (com.example.drinkder.BuildConfig.COCKTAIL_API_KEY.isBlank()) {
                        "Set API_KEY in .env and rebuild to discover drinks."
                    } else {
                        "Could not load this deck. Check your connection and try again."
                    }
                }
            } finally {
                if (generation == deckGeneration) isLoading.value = false
            }
        }
    }

    init {
        viewModelScope.launch {

            favoriteIds.clear()
            favoriteIds.addAll(favoritesStore.getIdsOnce())

            _favorites.value = loadFavoritesByIds(favoriteIds.toList())
        }
    }

    private suspend fun loadFavoritesByIds(ids: List<String>): List<Drink> {
        return withContext(Dispatchers.IO) {
            ids.map { id ->
                async {
                    try {
                        RetrofitInstance.api.getDrinkById(id).drinks?.firstOrNull()
                    } catch (_: Exception) {
                        null
                    }
                }
            }.mapNotNull { it.await() }
        }
    }



    fun swipeRight(drink: Drink) {
        saveFavorite(drink)
        removeDrink(drink)
    }

    fun saveFavorite(drink: Drink) {
        if (_favorites.value?.any { it.id == drink.id } == false) {

            _favorites.value = _favorites.value?.plus(drink)

            viewModelScope.launch { favoritesStore.add(drink.id) }
        }
    }

    fun swipeLeft(drink: Drink) {
        removeDrink(drink)
    }

    private fun removeDrink(drink: Drink) {

        _drinks.value = _drinks.value?.filterNot { it.id == drink.id }
    }

    fun removeFavorite(drink: Drink) {
        _favorites.value = _favorites.value?.filterNot { it.id == drink.id }
        viewModelScope.launch { favoritesStore.remove(drink.id) }
    }

    fun removeFavorites(drinks: List<Drink>) {
        if (drinks.isEmpty()) return

        val idsToRemove = drinks.map { it.id }.toSet()
        _favorites.value = _favorites.value?.filterNot { it.id in idsToRemove }
        viewModelScope.launch {
            val remainingIds = favoritesStore.getIdsOnce().toMutableSet()
            remainingIds.removeAll(idsToRemove)
            favoritesStore.setAll(remainingIds)
        }
    }

    fun clearFavorites() {
        _favorites.value = emptyList()
        viewModelScope.launch {
            favoritesStore.setAll(emptySet())
        }
    }

}
