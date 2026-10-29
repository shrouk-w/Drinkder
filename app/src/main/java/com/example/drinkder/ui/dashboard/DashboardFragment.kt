package com.example.drinkder.ui.dashboard

import SwipeViewModel
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ConcatAdapter
import com.example.drinkder.databinding.FragmentDashboardBinding

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val swipeViewModel: SwipeViewModel by activityViewModels()

    private lateinit var adapter: FavoritesAdapter
    private lateinit var headerAdapter: SavedHeaderAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)

        headerAdapter = SavedHeaderAdapter(
            onClear = { swipeViewModel.clearFavorites(); adapter.clearSelection() },
            onCancel = { adapter.clearSelection() },
            onRemoveSelected = {
                swipeViewModel.removeFavorites(adapter.getSelectedDrinks())
                adapter.clearSelection()
            }
        )

        adapter = FavoritesAdapter(
            onClick = { drink ->
                DrinkDetailsBottomSheet.newInstance(drink)
                    .show(parentFragmentManager, "drink_details")
            },
            onSelectionChanged = { count, inSelectionMode ->
                updateSelectionUi(count, inSelectionMode)
            }
        )

        val columns = if (resources.configuration.screenWidthDp >= 600) 3 else 2
        binding.favoritesRecycler.layoutManager = GridLayoutManager(requireContext(), columns).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int) = if (position == 0) columns else 1
            }
        }
        binding.favoritesRecycler.adapter = ConcatAdapter(headerAdapter, adapter)

        observeFavorites()

        return binding.root
    }

    private fun observeFavorites() {
        swipeViewModel.favorites.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
            headerAdapter.setFavoriteCount(list.size)
            binding.emptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            if (list.isEmpty()) {
                updateSelectionUi(0, false)
            }
        }
    }

    private fun updateSelectionUi(count: Int, inSelectionMode: Boolean) {
        headerAdapter.setSelection(count, inSelectionMode)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
