package org.liftoff.thepantry.data;

import org.liftoff.thepantry.models.RecipeIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.liftoff.thepantry.models.Recipe;

import java.util.List;

public interface RecipeIngredientRepository extends JpaRepository<RecipeIngredient, Integer> {
    List<RecipeIngredient> findByRecipeId(int recipe_id);
    List<RecipeIngredient> findByRecipeIdAndIngredientId(int recipe_id, int ingredient_id);
    List<RecipeIngredient> findByIdAndIngredientId(int id, int ingredient_id);
    List<RecipeIngredient> findByIngredientId(int ingredient_id);

    @Query("SELECT ri.recipe FROM RecipeIngredient ri " +
            "WHERE ri.ingredient.id IN :ingredientIds " +
            "GROUP BY ri.recipe " +
            "HAVING COUNT(DISTINCT ri.ingredient.id) = :ingredientCount " +
            "ORDER BY ri.recipe.name")
    List<Recipe> findRecipesContainingAllIngredients(
            @Param("ingredientIds") List<Integer> ingredientIds,
            @Param("ingredientCount") long ingredientCount);
}
