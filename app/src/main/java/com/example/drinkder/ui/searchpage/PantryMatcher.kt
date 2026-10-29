package com.example.drinkder.ui.searchpage

enum class MatchMode { EXACT, EXPLORE }

internal object PantryMatcher {
    data class Score(val matched: Int, val total: Int) {
        val complete: Boolean get() = total > 0 && matched == total
    }

    private val whitespace = Regex("\\s+")
    private fun normalize(value: String) = value.trim().lowercase().replace(whitespace, " ")

    fun matchesExactQuery(name: String, ingredients: List<String>, query: String): Boolean {
        val normalized = normalize(query)
        return normalized.isNotBlank() && (normalize(name) == normalized ||
            ingredients.any { normalize(it) == normalized })
    }

    fun score(ingredients: List<String>, pantry: Set<String>, mode: MatchMode): Score {
        val required = ingredients.map(::normalize).filter(String::isNotBlank).distinct()
        val available = pantry.map(::normalize).filter(String::isNotBlank).toSet()
        val matched = required.count { ingredient ->
            available.any { item ->
                if (mode == MatchMode.EXACT) ingredient == item
                else ingredient.contains(item) || item.contains(ingredient)
            }
        }
        return Score(matched, required.size)
    }
}
