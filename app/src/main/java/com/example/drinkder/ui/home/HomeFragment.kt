package com.example.drinkder.ui.home

import SwipeViewModel
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.drinkder.databinding.FragmentHomeBinding
import com.yuyakaido.android.cardstackview.*

class HomeFragment : Fragment(), CardStackListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SwipeViewModel by viewModels()

    private lateinit var cardStackLayoutManager: CardStackLayoutManager
    private lateinit var adapter: DrinkCardAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        (requireActivity() as AppCompatActivity).supportActionBar?.hide()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupCardStackView()

        viewModel.drinks.observe(viewLifecycleOwner) { drinks ->
            adapter.setDrinks(drinks)
            if (drinks.size < 4) viewModel.fetchDrink()
        }
    }

    private fun setupCardStackView() {
        cardStackLayoutManager = CardStackLayoutManager(requireContext(), this).apply {
            setStackFrom(StackFrom.Top)
            setVisibleCount(3)
            setTranslationInterval(8.0f)
            setScaleInterval(0.95f)
            setSwipeThreshold(0.3f)
            setMaxDegree(40.0f)
            setDirections(Direction.HORIZONTAL)
            setCanScrollVertical(false)
        }

        adapter = DrinkCardAdapter(emptyList())
        binding.cardStackView.layoutManager = cardStackLayoutManager
        binding.cardStackView.adapter = adapter
    }

    override fun onCardSwiped(direction: Direction?) {
        val currentDrink = viewModel.drinks.value?.firstOrNull()
        if (direction == Direction.Right && currentDrink != null) {
            viewModel.swipeRight(currentDrink)
        } else if (direction == Direction.Left && currentDrink != null) {
            viewModel.swipeLeft(currentDrink)
        }

        if ((viewModel.drinks.value?.size ?: 0) < 4) {
            viewModel.fetchDrink()
        }
    }

    override fun onCardRewound() {}
    override fun onCardDragging(direction: Direction?, ratio: Float) {}
    override fun onCardCanceled() {}
    override fun onCardAppeared(view: View?, position: Int) {}
    override fun onCardDisappeared(view: View?, position: Int) {}

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }




}
