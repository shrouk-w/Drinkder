package com.example.drinkder.ui.dashboard

import SwipeViewModel
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.drinkder.R
import com.example.drinkder.databinding.FragmentDashboardBinding

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val swipeViewModel: SwipeViewModel by activityViewModels()

    private lateinit var adapter: FavoritesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)

        adapter = FavoritesAdapter(
            onClick = { drink ->
                DrinkDetailsBottomSheet.newInstance(drink)
                    .show(parentFragmentManager, "drink_details")
            },
            onSelectionChanged = { count, inSelectionMode ->
                updateSelectionUi(count, inSelectionMode)
            }
        )

        binding.favoritesRecycler.layoutManager =
            GridLayoutManager(requireContext(), 2, RecyclerView.VERTICAL, false)
        binding.favoritesRecycler.adapter = adapter

        setupActions()
        observeFavorites()

        return binding.root
    }

    private fun setupActions() = with(binding) {
        clearAllButton.setOnClickListener {
            swipeViewModel.clearFavorites()
            adapter.clearSelection()
        }

        deleteSelectedButton.setOnClickListener {
            swipeViewModel.removeFavorites(adapter.getSelectedDrinks())
            adapter.clearSelection()
        }

        cancelSelectionButton.setOnClickListener {
            adapter.clearSelection()
        }
    }

    private fun observeFavorites() {
        swipeViewModel.favorites.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            binding.emptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            if (list.isEmpty()) {
                updateSelectionUi(0, false)
            }
        }
    }

    private fun updateSelectionUi(count: Int, inSelectionMode: Boolean) = with(binding) {
        defaultActions.visibility = if (inSelectionMode) View.GONE else View.VISIBLE
        selectionActions.visibility = if (inSelectionMode) View.VISIBLE else View.GONE
        deleteSelectedButton.isEnabled = count > 0
        selectionCount.text = resources.getQuantityString(
            R.plurals.saved_selected_count,
            count,
            count
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
