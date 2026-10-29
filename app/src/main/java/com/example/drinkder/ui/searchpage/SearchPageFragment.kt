package com.example.drinkder.ui.searchpage

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.inputmethod.EditorInfo
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.drinkder.databinding.SearchLayoutBinding
import com.example.drinkder.R
import com.example.drinkder.ui.dashboard.DrinkDetailsBottomSheet
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

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
        ViewCompat.setAccessibilityHeading(binding.searchTitle, true)
        setupResults()
        setupSearch()
        setupFilters()
        setupSearchCollapse()
        bindState()
    }

    private fun setupResults() {
        adapter = SearchResultsAdapter { drink ->
            DrinkDetailsBottomSheet.newInstance(drink)
                .show(parentFragmentManager, "search_drink_details")
        }

        binding.searchResults.layoutManager = GridLayoutManager(requireContext(),
            if (resources.configuration.screenWidthDp >= 600) 3 else 2)
        binding.searchResults.adapter = adapter
        val threshold = maxOf(ViewConfiguration.get(requireContext()).scaledTouchSlop.toFloat(),
            24 * resources.displayMetrics.density)
        binding.searchResults.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            private var downX = 0f
            private var downY = 0f
            override fun onInterceptTouchEvent(recycler: RecyclerView, event: MotionEvent): Boolean {
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    downX = event.x
                    downY = event.y
                } else if (event.actionMasked == MotionEvent.ACTION_MOVE && adapter.itemCount > 0 &&
                    viewModel.controlsExpanded.value == true) {
                    val dy = kotlin.math.abs(event.y - downY)
                    if (dy > threshold && dy > kotlin.math.abs(event.x - downX)) {
                        viewModel.controlsExpanded.value = false
                    }
                }
                return false
            }
        })
    }

    private fun setupSearch() = with(binding) {
        searchButton.setOnClickListener {
            ViewCompat.getWindowInsetsController(searchInput)?.hide(WindowInsetsCompat.Type.ime())
            searchInput.clearFocus()
            viewModel.search(searchInput.text?.toString().orEmpty())
        }

        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                ViewCompat.getWindowInsetsController(searchInput)?.hide(WindowInsetsCompat.Type.ime())
                searchInput.clearFocus()
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
        pantryInput.setOnItemClickListener { parent, _, position, _ ->
            viewModel.addPantryIngredient(parent.getItemAtPosition(position).toString())
            pantryInput.text?.clear()
            pantryInput.dismissDropDown()
        }
        barEditor.addOnLayoutChangeListener { _, left, _, right, _, _, _, _, _ ->
            if (right > left) pantryInput.dropDownWidth = right - left
        }

        retryButton.setOnClickListener { viewModel.retry() }
        searchModes.addOnButtonCheckedListener { _, checkedId, checked ->
            if (checked) viewModel.setSearchMode(
                if (checkedId == R.id.mode_bar) SearchPageViewModel.SearchMode.PANTRY
                else SearchPageViewModel.SearchMode.QUERY
            )
        }
    }

    private fun setupSearchCollapse() {
        binding.toggleSearchControls.setOnClickListener {
            viewModel.controlsExpanded.value = viewModel.controlsExpanded.value != true
        }
        viewModel.controlsExpanded.observe(viewLifecycleOwner) { expanded ->
            val list = binding.searchResults
            val layout = list.layoutManager as GridLayoutManager
            val first = layout.findFirstVisibleItemPosition()
            val offset = layout.findViewByPosition(first)?.top ?: 0
            binding.searchHeader.isVisible = expanded
            binding.toggleSearchControls.contentDescription = getString(
                if (expanded) R.string.hide_search_controls else R.string.show_search_controls
            )
            ViewCompat.setStateDescription(binding.toggleSearchControls, getString(
                if (expanded) R.string.controls_expanded else R.string.controls_collapsed
            ))
            if (!expanded) {
                binding.pantryInput.dismissDropDown()
                binding.pantryInput.clearFocus()
                binding.searchInput.clearFocus()
                ViewCompat.getWindowInsetsController(binding.root)?.hide(WindowInsetsCompat.Type.ime())
            }
            if (first >= 0) list.post {
                if (_binding?.searchResults === list) layout.scrollToPositionWithOffset(first, offset)
            }
        }
    }

    private fun setupFilters() = with(binding) {
        alcoholFilterButton.setOnClickListener {
            val next = when (viewModel.alcoholFilter.value) {
                SearchPageViewModel.AlcoholFilter.ALL -> SearchPageViewModel.AlcoholFilter.ALCOHOLIC
                SearchPageViewModel.AlcoholFilter.ALCOHOLIC -> SearchPageViewModel.AlcoholFilter.NON_ALCOHOLIC
                else -> SearchPageViewModel.AlcoholFilter.ALL
            }
            viewModel.setAlcoholFilter(next)
        }
        matchFilterButton.setOnClickListener {
            viewModel.setMatchMode(if (viewModel.matchMode.value == MatchMode.EXPLORE)
                MatchMode.EXACT else MatchMode.EXPLORE)
        }
    }

    private fun addPantryItem() {
        val value = binding.pantryInput.text?.toString().orEmpty()
        viewModel.addPantryIngredient(value)
        binding.pantryInput.text?.clear()
    }

    private fun bindState() {
        viewModel.ingredientNames.observe(viewLifecycleOwner) { names ->
            val input = binding.pantryInput
            val suggestions = IngredientSuggestionsAdapter(requireContext(), names)
            input.setAdapter(suggestions)
            if (input.hasFocus() && !input.text.isNullOrBlank()) {
                suggestions.filter.filter(input.text) { count ->
                    if (count > 0 && input.isAttachedToWindow && input.hasFocus() && input.isShown) {
                        input.showDropDown()
                    }
                }
            }
        }
        viewModel.warning.observe(viewLifecycleOwner) { warning ->
            if (warning != null) {
                Snackbar.make(binding.root, warning, Snackbar.LENGTH_LONG)
                    .setAction(R.string.try_again) { viewModel.retry() }
                    .show()
                viewModel.warning.value = null
            }
        }
        viewModel.results.observe(viewLifecycleOwner) { drinks ->
            adapter.submitList(drinks)
        }

        viewModel.pantryItems.observe(viewLifecycleOwner) { items ->
            renderPantryChips(items)
        }

        viewModel.alcoholFilter.observe(viewLifecycleOwner) { filter ->
            binding.alcoholFilterButton.setText(when (filter) {
                SearchPageViewModel.AlcoholFilter.ALCOHOLIC -> R.string.filter_alcoholic
                SearchPageViewModel.AlcoholFilter.NON_ALCOHOLIC -> R.string.filter_non_alcoholic
                else -> R.string.filter_all
            })
            binding.alcoholFilterButton.contentDescription = getString(
                R.string.cycle_alcohol_filter, binding.alcoholFilterButton.text
            )
        }

        viewModel.searchMode.observe(viewLifecycleOwner) { mode ->
            val pantry = mode == SearchPageViewModel.SearchMode.PANTRY
            binding.queryRow.isVisible = !pantry
            binding.barEditor.isVisible = pantry
            binding.pantryDetails.isVisible = pantry
            binding.searchModes.check(if (pantry) R.id.mode_bar else R.id.mode_query)
        }

        viewModel.matchMode.observe(viewLifecycleOwner) { mode ->
            val exact = mode == MatchMode.EXACT
            binding.matchFilterButton.setText(if (exact) R.string.match_exact else R.string.match_explore)
            binding.matchFilterButton.contentDescription = getString(
                R.string.cycle_match_filter, binding.matchFilterButton.text
            )
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.searchProgress.isVisible = isLoading
            binding.stateCard.isVisible = isLoading || binding.stateMessage.text.isNotBlank()
            binding.retryButton.isVisible =
                !isLoading && viewModel.hasError.value == true
            binding.searchButton.isEnabled = !isLoading
        }

        viewModel.message.observe(viewLifecycleOwner) { message ->
            binding.stateMessage.text = message
            val showMessage = message.isNotBlank()
            binding.stateCard.isVisible = showMessage || viewModel.isLoading.value == true
            binding.retryButton.isVisible =
                showMessage && viewModel.isLoading.value != true && viewModel.hasError.value == true
        }
    }

    private fun renderPantryChips(items: List<String>) {
        binding.pantryChips.removeAllViews()
        items.forEach { item ->
            val chip = Chip(requireContext()).apply {
                text = item.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                isCloseIconVisible = true
                closeIconTint = ContextCompat.getColorStateList(context, R.color.on_background)
                chipBackgroundColor = ContextCompat.getColorStateList(context, R.color.bar_chip_background)
                chipStrokeColor = ContextCompat.getColorStateList(context, R.color.glass_line)
                chipStrokeWidth = resources.displayMetrics.density
                setTextColor(ContextCompat.getColor(context, R.color.on_background))
                setOnCloseIconClickListener {
                    viewModel.removePantryIngredient(item)
                }
            }
            binding.pantryChips.addView(chip)
        }
    }

    override fun onDestroyView() {
        _binding?.pantryInput?.dismissDropDown()
        super.onDestroyView()
        _binding = null
    }
}
