package com.example.drinkder.ui.searchpage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.example.drinkder.databinding.SearchLayoutBinding
import com.example.drinkder.ui.dashboard.DrinkDetailsBottomSheet
import com.google.android.material.chip.Chip
import androidx.core.content.ContextCompat

class SearchPageFragment : Fragment() {

    private var _binding: SearchLayoutBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SearchPageViewModel by viewModels()
    private lateinit var adapter: SearchResultsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SearchLayoutBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupResults()
        setupSearch()
        setupFilters()
        bindState()
    }

    private fun setupResults() {
        adapter = SearchResultsAdapter { drink ->
            DrinkDetailsBottomSheet.newInstance(drink)
                .show(parentFragmentManager, "search_drink_details")
        }

        binding.searchResults.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.searchResults.adapter = adapter
    }

    private fun setupSearch() = with(binding) {
        searchButton.setOnClickListener {
            viewModel.search(searchInput.text?.toString().orEmpty())
        }

        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                viewModel.search(searchInput.text?.toString().orEmpty())
                true
            } else {
                false
            }
        }

        pantryInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addPantryItem()
                true
            } else {
                false
            }
        }

        addPantryButton.setOnClickListener {
            addPantryItem()
        }

        retryButton.setOnClickListener { viewModel.retry() }
    }

    private fun setupFilters() = with(binding) {
        filterAll.setOnClickListener {
            setAlcoholFilter(SearchPageViewModel.AlcoholFilter.ALL)
        }

        filterAlcoholic.setOnClickListener {
            setAlcoholFilter(SearchPageViewModel.AlcoholFilter.ALCOHOLIC)
        }

        filterNonAlcoholic.setOnClickListener {
            setAlcoholFilter(SearchPageViewModel.AlcoholFilter.NON_ALCOHOLIC)
        }

        filterPantryOnly.setOnCheckedChangeListener { _, checked ->
            viewModel.setPantryOnly(checked)
        }
    }

    private fun setAlcoholFilter(filter: SearchPageViewModel.AlcoholFilter) = with(binding) {
        filterAll.isChecked = filter == SearchPageViewModel.AlcoholFilter.ALL
        filterAlcoholic.isChecked = filter == SearchPageViewModel.AlcoholFilter.ALCOHOLIC
        filterNonAlcoholic.isChecked = filter == SearchPageViewModel.AlcoholFilter.NON_ALCOHOLIC
        viewModel.setAlcoholFilter(filter)
    }

    private fun addPantryItem() {
        val value = binding.pantryInput.text?.toString().orEmpty()
        viewModel.addPantryIngredient(value)
        binding.pantryInput.text?.clear()
    }

    private fun bindState() {
        viewModel.results.observe(viewLifecycleOwner) { drinks ->
            adapter.submitList(drinks)
        }

        viewModel.pantryItems.observe(viewLifecycleOwner) { items ->
            renderPantryChips(items)
        }

        viewModel.alcoholFilter.observe(viewLifecycleOwner) { filter ->
            binding.filterAll.isChecked = filter == SearchPageViewModel.AlcoholFilter.ALL
            binding.filterAlcoholic.isChecked = filter == SearchPageViewModel.AlcoholFilter.ALCOHOLIC
            binding.filterNonAlcoholic.isChecked = filter == SearchPageViewModel.AlcoholFilter.NON_ALCOHOLIC
        }

        viewModel.pantryOnly.observe(viewLifecycleOwner) { enabled ->
            binding.filterPantryOnly.isChecked = enabled
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.searchProgress.isVisible = isLoading
            binding.stateCard.isVisible = isLoading || binding.stateMessage.text.isNotBlank()
            binding.retryButton.isVisible =
                !isLoading && binding.stateMessage.text.isNotBlank() && binding.searchInput.text?.isNotBlank() == true
        }

        viewModel.message.observe(viewLifecycleOwner) { message ->
            binding.stateMessage.text = message
            val showMessage = message.isNotBlank()
            binding.stateCard.isVisible = showMessage || viewModel.isLoading.value == true
            binding.retryButton.isVisible =
                showMessage && !viewModel.isLoading.value!! && binding.searchInput.text?.isNotBlank() == true
        }
    }

    private fun renderPantryChips(items: List<String>) {
        binding.pantryChips.removeAllViews()
        items.forEach { item ->
            val chip = Chip(requireContext()).apply {
                text = item.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                isCloseIconVisible = true
                closeIconTint = resources.getColorStateList(com.example.drinkder.R.color.apricot_300, null)
                chipBackgroundColor = ContextCompat.getColorStateList(context, com.example.drinkder.R.color.search_chip_bg)
                chipStrokeColor = ContextCompat.getColorStateList(context, com.example.drinkder.R.color.search_chip_stroke)
                chipStrokeWidth = resources.displayMetrics.density
                setTextColor(ContextCompat.getColor(context, com.example.drinkder.R.color.cream_100))
                setOnCloseIconClickListener {
                    viewModel.removePantryIngredient(item)
                }
            }
            binding.pantryChips.addView(chip)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
