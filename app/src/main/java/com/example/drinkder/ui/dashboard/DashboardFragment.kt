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
import com.example.drinkder.databinding.FragmentDashboardBinding
import com.example.drinkder.model.Drink

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

        adapter = FavoritesAdapter(onClick = { drink ->
            DrinkDetailsBottomSheet.newInstance(drink)
                .show(parentFragmentManager, "drink_details")
        })
        binding.favoritesRecycler.layoutManager =
            GridLayoutManager(requireContext(), 2, RecyclerView.VERTICAL, false)
        binding.favoritesRecycler.adapter = adapter

        // obserwuj ulubione i podawaj do adaptera
        swipeViewModel.favorites.observe(viewLifecycleOwner) { list ->
            adapter.submitList(list)
        }

        return binding.root
    }

    private fun onDrinkClicked(drink: Drink) {
        // tu możesz otworzyć szczegóły, Toast, nawigację – opcjonalnie
        // Toast.makeText(requireContext(), drink.name, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}