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
            binding.drinkDescription.text = drink.description
            binding.drinkImage.load(drink.imageUrl)
        }
    }
}
