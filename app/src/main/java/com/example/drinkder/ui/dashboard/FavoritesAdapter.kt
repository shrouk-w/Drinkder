package com.example.drinkder.ui.dashboard

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.drinkder.databinding.ItemFavoriteDrinkBinding
import com.example.drinkder.model.Drink

class FavoritesAdapter(
    private val onClick: (Drink) -> Unit = {},
    private val onSelectionChanged: (Int, Boolean) -> Unit = { _, _ -> }
) : ListAdapter<Drink, FavoritesAdapter.VH>(DIFF) {

    private val selectedIds = linkedSetOf<String>()
    private var selectionMode = false

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

            val isSelected = selectedIds.contains(item.id)
            selectionOverlay.visibility = if (isSelected) View.VISIBLE else View.GONE
            selectionCheck.visibility = if (isSelected) View.VISIBLE else View.GONE
            root.alpha = if (selectionMode && !isSelected) 0.78f else 1f

            root.setOnClickListener {
                if (selectionMode) {
                    toggleSelection(item)
                } else {
                    onClick(item)
                }
            }

            root.setOnLongClickListener {
                toggleSelection(item)
                true
            }
        }
    }

    override fun onCurrentListChanged(
        previousList: MutableList<Drink>,
        currentList: MutableList<Drink>
    ) {
        super.onCurrentListChanged(previousList, currentList)
        val validIds = currentList.mapTo(mutableSetOf()) { it.id }
        selectedIds.retainAll(validIds)
        selectionMode = selectedIds.isNotEmpty()
        notifyDataSetChanged()
        dispatchSelectionChanged()
    }

    fun clearSelection() {
        if (selectedIds.isEmpty()) return
        selectedIds.clear()
        selectionMode = false
        notifyDataSetChanged()
        dispatchSelectionChanged()
    }

    fun getSelectedDrinks(): List<Drink> = currentList.filter { selectedIds.contains(it.id) }

    private fun toggleSelection(drink: Drink) {
        if (selectedIds.contains(drink.id)) {
            selectedIds.remove(drink.id)
        } else {
            selectedIds.add(drink.id)
        }
        selectionMode = selectedIds.isNotEmpty()
        notifyDataSetChanged()
        dispatchSelectionChanged()
    }

    private fun dispatchSelectionChanged() {
        onSelectionChanged(selectedIds.size, selectionMode)
    }
}
