package com.example.drinkder.ui.dashboard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.drinkder.R
import com.example.drinkder.databinding.ItemSavedHeaderBinding

class SavedHeaderAdapter(
    private val onClear: () -> Unit,
    private val onCancel: () -> Unit,
    private val onRemoveSelected: () -> Unit
) : RecyclerView.Adapter<SavedHeaderAdapter.Holder>() {
    private var favoriteCount = 0
    private var selectedCount = 0
    private var selecting = false

    class Holder(val binding: ItemSavedHeaderBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemCount() = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(
        ItemSavedHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: Holder, position: Int) = with(holder.binding) {
        ViewCompat.setAccessibilityHeading(savedTitle, true)
        clearAllAction.isVisible = !selecting
        clearAllButton.isEnabled = favoriteCount > 0
        clearAllAction.alpha = if (favoriteCount > 0) 1f else 0.4f
        clearAllButton.setOnClickListener { onClear() }
        selectionActions.isVisible = selecting
        selectionCount.text = root.resources.getQuantityString(
            R.plurals.saved_selected_count, selectedCount, selectedCount
        )
        cancelSelectionButton.setOnClickListener { onCancel() }
        deleteSelectedButton.isEnabled = selectedCount > 0
        deleteSelectedButton.setOnClickListener { onRemoveSelected() }
    }

    fun setFavoriteCount(count: Int) {
        favoriteCount = count
        notifyItemChanged(0)
    }

    fun setSelection(count: Int, active: Boolean) {
        selectedCount = count
        selecting = active
        notifyItemChanged(0)
    }
}
