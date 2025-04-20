package com.example.drinkder.ui.home

import SwipeTouchListener
import SwipeViewModel
import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import coil.load
import com.example.drinkder.R
import com.example.drinkder.databinding.FragmentHomeBinding
import com.example.drinkder.model.Drink

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SwipeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).supportActionBar?.hide()

        return binding.root
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //(requireActivity() as AppCompatActivity).setSupportActionBar(view.findViewById(R.id.customToolbar))
        //(requireActivity() as AppCompatActivity).supportActionBar?.setDisplayShowTitleEnabled(true) //<-- needs fixing for custom toolbar to work, now crashes


        viewModel.drinks.observe(viewLifecycleOwner) { drinks ->
            if (drinks.isEmpty()) {
                viewModel.fetchDrink()
            } else {
                showDrink(drinks.first())
            }
        }

        binding.drinkCard.setOnTouchListener(SwipeTouchListener(
            requireContext(),
            onSwipeLeft = {
                viewModel.drinks.value?.firstOrNull()?.let { viewModel.swipeLeft(it) }
            },
            onSwipeRight = {
                viewModel.drinks.value?.firstOrNull()?.let { viewModel.swipeRight(it) }
            }

        ))
    }

    private fun showDrink(drink: Drink) {
        binding.drinkTitle.text = drink.name
        binding.drinkInstructions.text = drink.description
        binding.drinkImage.load(drink.imageUrl)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
