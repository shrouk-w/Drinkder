package com.example.drinkder.ui.home

import SwipeViewModel
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.drinkder.databinding.FragmentHomeBinding
import com.example.drinkder.R
import com.example.drinkder.ui.dashboard.DrinkDetailsBottomSheet
import androidx.core.view.isVisible
import com.yuyakaido.android.cardstackview.*

class HomeFragment : Fragment(), CardStackListener {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SwipeViewModel by activityViewModels()

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
        binding.skipButton.setOnClickListener { swipe(Direction.Left) }
        binding.likeButton.setOnClickListener { swipe(Direction.Right) }
        binding.detailsButton.setOnClickListener {
            viewModel.drinks.value?.firstOrNull()?.let { drink ->
                DrinkDetailsBottomSheet.newInstance(drink)
                    .show(parentFragmentManager, "discovery_recipe")
            }
        }
        binding.homeRetry.setOnClickListener { viewModel.fetchDrink() }

        viewModel.drinks.observe(viewLifecycleOwner) { drinks ->
            adapter.setDrinks(drinks)
            binding.cardStackView.scrollToPosition(0)
            updateState()
        }
        viewModel.isLoading.observe(viewLifecycleOwner) { updateState() }
        viewModel.error.observe(viewLifecycleOwner) { updateState() }
        viewModel.fetchDrink()
    }

    private fun updateState() = with(binding) {
        val empty = viewModel.drinks.value.isNullOrEmpty()
        homeState.isVisible = empty
        homeProgress.isVisible = viewModel.isLoading.value == true
        homeMessage.text = viewModel.error.value ?: getString(R.string.home_loading)
        homeRetry.isVisible = viewModel.error.value != null && viewModel.isLoading.value != true
        listOf(skipButton, likeButton, detailsButton).forEach {
            it.isEnabled = !empty
            it.alpha = if (empty) 0.4f else 1f
        }
    }

    private fun swipe(direction: Direction) {
        if (viewModel.drinks.value.isNullOrEmpty()) return
        val duration = if (android.os.Build.VERSION.SDK_INT >= 26 &&
            !android.animation.ValueAnimator.areAnimatorsEnabled()) 0 else 220
        cardStackLayoutManager.setSwipeAnimationSetting(
            SwipeAnimationSetting.Builder().setDirection(direction).setDuration(duration).build()
        )
        binding.cardStackView.swipe()
    }

    private fun setupCardStackView() {
        cardStackLayoutManager = CardStackLayoutManager(requireContext(), this).apply {
            setStackFrom(StackFrom.None)
            setVisibleCount(1)
            setTranslationInterval(8.0f)
            setScaleInterval(0.95f)
            setSwipeThreshold(0.3f)
            setMaxDegree(18.0f)
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
