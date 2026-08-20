package org.liftoff.thepantry.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecipeSeedDataTests {

    @Test
    void curatedSeedBatchHasUniqueRecipesAndBundledImages() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        RecipeSeedService.SeedRecipe[] recipes = objectMapper.readValue(
                new ClassPathResource("seed/recipes.json").getInputStream(),
                RecipeSeedService.SeedRecipe[].class);

        assertEquals(20, recipes.length);

        Set<String> names = new HashSet<>();
        Arrays.stream(recipes).forEach(recipe -> {
            assertTrue(names.add(recipe.name), "Duplicate recipe: " + recipe.name);
            assertFalse(recipe.ingredients.isEmpty(), "Missing ingredients: " + recipe.name);
            assertTrue(new ClassPathResource("static/images/" + recipe.image).exists(),
                    "Missing image: " + recipe.image);
        });
    }
}
