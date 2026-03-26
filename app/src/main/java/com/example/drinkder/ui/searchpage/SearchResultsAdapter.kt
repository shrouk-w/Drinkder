package com.example.drinkder.ui.searchpage

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.drinkder.databinding.ItemFavoriteDrinkBinding
import com.example.drinkder.model.Drink

class SearchResultsAdapter(
    private val onClick: (Drink) -> Unit
) : ListAdapter<Drink, SearchResultsAdapter.SearchResultViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultViewHolder {
        val binding = ItemFavoriteDrinkBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SearchResultViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SearchResultViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SearchResultViewHolder(
        private val binding: ItemFavoriteDrinkBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(drink: Drink) = with(binding) {
            textName.text = drink.name
            imageThumb.load(drink.imageUrl)
            root.setOnClickListener { onClick(drink) }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Drink>() {
            override fun areItemsTheSame(oldItem: Drink, newItem: Drink): Boolean = oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Drink, newItem: Drink): Boolean = oldItem == newItem
        }
    }
}
