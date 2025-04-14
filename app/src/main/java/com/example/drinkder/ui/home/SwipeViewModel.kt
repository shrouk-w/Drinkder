import android.util.Log
import androidx.lifecycle.*
import com.example.drinkder.model.Drink
import com.example.drinkder.network.RetrofitInstance
import kotlinx.coroutines.launch

class SwipeViewModel : ViewModel() {
    private val _drinks = MutableLiveData<List<Drink>>(emptyList())
    val drinks: LiveData<List<Drink>> = _drinks

    private val _favorites = MutableLiveData<List<Drink>>(emptyList())
    val favorites: LiveData<List<Drink>> = _favorites

    fun fetchDrink() {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.getRandomDrink()
                val newDrink = response.drinks.firstOrNull()
                newDrink?.let {
                    _drinks.value = _drinks.value?.plus(it)
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
