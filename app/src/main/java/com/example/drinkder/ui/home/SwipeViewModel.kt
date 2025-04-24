import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import coil.Coil
import coil.request.ImageRequest
import com.example.drinkder.model.Drink
import com.example.drinkder.network.RetrofitInstance
import kotlinx.coroutines.launch

class SwipeViewModel(application: Application, savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {
    private val _drinks = savedStateHandle.getLiveData<List<Drink>>("drinks", emptyList())
    val drinks: LiveData<List<Drink>> = _drinks

    private val _favorites = savedStateHandle.getLiveData<List<Drink>>("favorites", emptyList())
    val favorites: LiveData<List<Drink>> = _favorites

    fun fetchDrink() {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.getRandomDrink()
                val newDrink = response.drinks.firstOrNull()
                newDrink?.let { drink ->

                    val request = ImageRequest.Builder(context = getApplication<Application>().applicationContext)
                        .data(drink.imageUrl)
                        .build()

                    Coil.imageLoader(getApplication()).enqueue(request)

                    _drinks.value = _drinks.value?.plus(drink)
                }
            } catch (e: Exception) {
                Log.e("SwipeViewModel", "Error fetching drink: ${e.message}", e)
            }
        }
    }


    fun swipeRight(drink: Drink) {
        if (_favorites.value?.any { it.id == drink.id } == false) {
            _favorites.value = _favorites.value?.plus(drink)
        }
        removeDrink(drink)
    }

    fun swipeLeft(drink: Drink) {
        removeDrink(drink)
    }

    private fun removeDrink(drink: Drink) {
        _drinks.value = _drinks.value?.filterNot { it.id == drink.id }
    }
}
