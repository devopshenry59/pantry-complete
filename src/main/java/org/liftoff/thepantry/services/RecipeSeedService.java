package org.liftoff.thepantry.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.liftoff.thepantry.data.IngredientRepository;
import org.liftoff.thepantry.data.RecipeRepository;
import org.liftoff.thepantry.data.UnitRepository;
import org.liftoff.thepantry.models.Ingredient;
import org.liftoff.thepantry.models.Recipe;
import org.liftoff.thepantry.models.RecipeIngredient;
import org.liftoff.thepantry.models.Unit;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
@Profile("seed")
public class RecipeSeedService implements ApplicationRunner {

    private final RecipeRepository recipeRepository;
    private final IngredientRepository ingredientRepository;
    private final UnitRepository unitRepository;
    private final ObjectMapper objectMapper;
    private final QuantityNormalizationService quantityNormalizationService;

    public RecipeSeedService(RecipeRepository recipeRepository,
                             IngredientRepository ingredientRepository,
                             UnitRepository unitRepository,
                             ObjectMapper objectMapper,
                             QuantityNormalizationService quantityNormalizationService) {
        this.recipeRepository = recipeRepository;
        this.ingredientRepository = ingredientRepository;
        this.unitRepository = unitRepository;
        this.objectMapper = objectMapper;
        this.quantityNormalizationService = quantityNormalizationService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        SeedRecipe[] seeds = objectMapper.readValue(
                new ClassPathResource("seed/recipes.json").getInputStream(), SeedRecipe[].class);

        Arrays.stream(seeds)
                .filter(seed -> recipeRepository.findByName(seed.name).isEmpty())
                .forEach(this::saveRecipe);
    }

    private void saveRecipe(SeedRecipe seed) {
        Recipe recipe = new Recipe(seed.name, seed.description, seed.instructions, seed.image, null);
        for (SeedIngredient item : seed.ingredients) {
            Ingredient ingredient = findIngredient(item.name);
            Unit unit = item.unit == null || item.unit.isBlank() ? null : findUnit(item.unit);
            RecipeIngredient recipeIngredient = new RecipeIngredient(item.amount, recipe, ingredient, unit);
            quantityNormalizationService.normalize(item.amount, unit)
                    .ifPresent(quantity -> recipeIngredient.setNormalizedAmount(quantity.getAmount()));
            recipe.getRecipeIngredients().add(recipeIngredient);
        }
        recipeRepository.save(recipe);
    }

    private Ingredient findIngredient(String name) {
        List<Ingredient> matches = ingredientRepository.findByName(name);
        return matches.isEmpty() ? ingredientRepository.save(new Ingredient(name)) : matches.get(0);
    }

    private Unit findUnit(String name) {
        List<Unit> matches = unitRepository.findByName(name);
        return matches.isEmpty() ? unitRepository.save(new Unit(name)) : matches.get(0);
    }

    public static class SeedRecipe {
        public String name;
        public String description;
        public String instructions;
        public String image;
        public List<SeedIngredient> ingredients;
    }

    public static class SeedIngredient {
        public String amount;
        public String unit;
        public String name;
    }
}
