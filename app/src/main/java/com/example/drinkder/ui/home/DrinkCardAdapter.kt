package com.example.drinkder.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.drinkder.databinding.ItemDrinkBinding
import com.example.drinkder.model.Drink

class DrinkCardAdapter(
    private var drinks: List<Drink>
) : RecyclerView.Adapter<DrinkCardAdapter.DrinkViewHolder>() {

    fun setDrinks(newDrinks: List<Drink>) {
        drinks = newDrinks
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DrinkViewHolder {
        val binding = ItemDrinkBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DrinkViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DrinkViewHolder, position: Int) {
        holder.bind(drinks[position])
    }

    override fun getItemCount(): Int = drinks.size

    class DrinkViewHolder(private val binding: ItemDrinkBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(drink: Drink) {
            binding.drinkName.text = drink.name
            val ingredients = listOf(
                drink.ingredient1,
                drink.ingredient2,
                drink.ingredient3,
                drink.ingredient4,
                drink.ingredient5,
                drink.ingredient6,
                drink.ingredient7,
                drink.ingredient8,
                drink.ingredient9,
                drink.ingredient10,
                drink.ingredient11,
                drink.ingredient12,
                drink.ingredient13,
                drink.ingredient14,
                drink.ingredient15
            ).filterNot { it.isNullOrBlank() }
                .mapIndexed{i, ing ->  "${i + 1}. $ing"}

            binding.drinkDescription.text = ingredients.joinToString(separator = "\n")
            binding.drinkImage.load(drink.imageUrl)
        }
    }
}
