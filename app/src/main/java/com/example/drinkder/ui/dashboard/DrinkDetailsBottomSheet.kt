package com.example.drinkder.ui.dashboard


import SwipeViewModel
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.activityViewModels
import coil.load
import com.example.drinkder.databinding.FragmentDrinkDetailsBottomSheetBinding
import com.example.drinkder.model.Drink
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class DrinkDetailsBottomSheet : BottomSheetDialogFragment() {

    private var _binding: FragmentDrinkDetailsBottomSheetBinding? = null
    private val binding get() = _binding!!

    private val swipeViewModel: SwipeViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDrinkDetailsBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)

        dialog.setOnShowListener { dlg ->
            val bottom = (dlg as BottomSheetDialog)
                .findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
                ?: return@setOnShowListener

            bottom.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT

            val behavior = BottomSheetBehavior.from(bottom).apply {
                isFitToContents = false
                skipCollapsed = true
                val screen = resources.displayMetrics.heightPixels
                expandedOffset = (screen * 0.25f).toInt()
                state = BottomSheetBehavior.STATE_EXPANDED
            }
        }

        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val drink = requireArguments().getSerializable(ARG_DRINK) as Drink

        binding.title.text = drink.name
        binding.imageLarge.load(drink.imageUrl)
        binding.instructions.text = drink.description.ifBlank { "No instructions available." }
        binding.ingredients.text = buildIngredients(drink)

        binding.btnClose.setOnClickListener { dismiss() }
        binding.btnDelete.setOnClickListener {
            swipeViewModel.removeFavorite(drink)
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_DRINK = "arg_drink"

        fun newInstance(drink: Drink) = DrinkDetailsBottomSheet().apply {
            arguments = Bundle().apply { putSerializable(ARG_DRINK, drink) }
        }
    }

    private fun buildIngredients(drink: Drink): String {
        val ingredients = listOf(
            drink.ingredient1 to drink.measure1,
            drink.ingredient2 to drink.measure2,
            drink.ingredient3 to drink.measure3,
            drink.ingredient4 to drink.measure4,
            drink.ingredient5 to drink.measure5,
            drink.ingredient6 to drink.measure6,
            drink.ingredient7 to drink.measure7,
            drink.ingredient8 to drink.measure8,
            drink.ingredient9 to drink.measure9,
            drink.ingredient10 to drink.measure10,
            drink.ingredient11 to drink.measure11,
            drink.ingredient12 to drink.measure12,
            drink.ingredient13 to drink.measure13,
            drink.ingredient14 to drink.measure14,
            drink.ingredient15 to drink.measure15
        )

        return ingredients
            .filter { !it.first.isNullOrBlank() }
            .joinToString(separator = "\n") { (ingredient, measure) ->
                listOfNotNull(measure?.trim()?.takeIf { it.isNotEmpty() }, ingredient?.trim())
                    .joinToString(" ")
            }
            .ifBlank { "No ingredient list available." }
    }
}
