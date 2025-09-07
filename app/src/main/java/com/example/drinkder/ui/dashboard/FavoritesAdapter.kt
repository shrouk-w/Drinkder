package com.example.drinkder.ui.dashboard


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.drinkder.databinding.ItemFavoriteDrinkBinding
import com.example.drinkder.model.Drink

class FavoritesAdapter(
    private val onClick: (Drink) -> Unit = {}
) : ListAdapter<Drink, FavoritesAdapter.VH>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Drink>() {
            override fun areItemsTheSame(oldItem: Drink, newItem: Drink) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Drink, newItem: Drink) = oldItem == newItem
        }
    }

    inner class VH(val binding: ItemFavoriteDrinkBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemFavoriteDrinkBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        with(holder.binding) {
            textName.text = item.name
            imageThumb.load(item.imageUrl)
            root.setOnClickListener { onClick(item) }
        }
    }
}