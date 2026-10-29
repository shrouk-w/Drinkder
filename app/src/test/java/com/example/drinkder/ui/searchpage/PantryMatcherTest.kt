package com.example.drinkder.ui.searchpage

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PantryMatcherTest {
    @Test fun exactCocktailNameExcludesVariants() {
        assertTrue(PantryMatcher.matchesExactQuery("Margarita", listOf("Tequila", "Lime juice"), " margarita "))
        assertFalse(PantryMatcher.matchesExactQuery("Blue Margarita", listOf("Tequila"), "margarita"))
        assertFalse(PantryMatcher.matchesExactQuery("Tommy's Margarita", listOf("Tequila"), "margarita"))
    }

    @Test fun exactIngredientSearchAllowsRecipesUsingThatIngredient() {
        assertTrue(PantryMatcher.matchesExactQuery("Gin Fizz", listOf("Gin", "Lemon"), "GIN"))
        assertFalse(PantryMatcher.matchesExactQuery("Ginger tea", listOf("Ginger"), "gin"))
        assertFalse(PantryMatcher.matchesExactQuery("Sloe Gin Fizz", listOf("Sloe gin"), "gin"))
    }

    @Test fun exactAllowsUnusedPantryIngredients() {
        val score = PantryMatcher.score(
            listOf("Gin", "Tonic water"), setOf("gin", "tonic water", "lime"), MatchMode.EXACT
        )
        assertTrue(score.complete)
    }

    @Test fun exactRejectsEvenOneMissingIngredient() {
        assertFalse(PantryMatcher.score(
            listOf("Gin", "Tonic water"), setOf("gin"), MatchMode.EXACT
        ).complete)
    }

    @Test fun exactNormalizesCaseAndSpacing() {
        assertTrue(PantryMatcher.score(
            listOf("  Tonic   Water ", "GIN"), setOf("tonic water", " gin "), MatchMode.EXACT
        ).complete)
    }

    @Test fun exactDoesNotConfuseGinAndGingerOrLimeAndLimeJuice() {
        assertFalse(PantryMatcher.score(
            listOf("Ginger"), setOf("gin"), MatchMode.EXACT
        ).complete)
        assertFalse(PantryMatcher.score(
            listOf("Lime juice"), setOf("lime"), MatchMode.EXACT
        ).complete)
    }

    @Test fun emptyRecipeCannotBeAnExactMatch() {
        assertFalse(PantryMatcher.score(emptyList(), setOf("gin"), MatchMode.EXACT).complete)
    }

    @Test fun exploreAllowsPartialMatchesAndRetainsBroadIngredientSearch() {
        val score = PantryMatcher.score(
            listOf("Light rum", "Cola"), setOf("rum"), MatchMode.EXPLORE
        )
        assertTrue(score.matched > 0)
        assertFalse(score.complete)
    }
}
