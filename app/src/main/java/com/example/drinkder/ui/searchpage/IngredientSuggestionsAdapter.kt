package com.example.drinkder.ui.searchpage

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter
import com.example.drinkder.R

class IngredientSuggestionsAdapter(context: Context, private val names: List<String>) :
    ArrayAdapter<String>(context, R.layout.item_ingredient_suggestion, ArrayList(names)) {

    private val suggestions = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val query = constraint?.toString()?.trim()?.lowercase().orEmpty()
            val matches = if (query.isEmpty()) emptyList() else names
                .filter { it.contains(query, ignoreCase = true) }
                .sortedWith(compareBy<String> {
                    when {
                        it.equals(query, ignoreCase = true) -> 0
                        it.startsWith(query, ignoreCase = true) -> 1
                        else -> 2
                    }
                }.thenBy { it.length }.thenBy { it.lowercase() })
            return FilterResults().apply { values = matches; count = matches.size }
        }

        override fun publishResults(constraint: CharSequence?, results: FilterResults) {
            setNotifyOnChange(false)
            clear()
            addAll((results.values as? List<*>)?.filterIsInstance<String>().orEmpty())
            notifyDataSetChanged()
        }
    }

    override fun getFilter(): Filter = suggestions
}
