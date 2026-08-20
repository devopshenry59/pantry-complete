package org.liftoff.thepantry.models;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotBlank;
import javax.persistence.Column;
import java.math.BigDecimal;

@Entity
public class RecipeIngredient extends AbstractEntity {

    @NotBlank
    private String amount;

    @Column(precision = 19, scale = 6)
    private BigDecimal normalizedAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    private Unit unit;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    public RecipeIngredient() {
    }

    public RecipeIngredient(String amount, Recipe recipe, Ingredient ingredient, Unit unit) {
        this.amount = amount;
        this.recipe = recipe;
        this.ingredient = ingredient;
        this.unit = unit;
    }

    public String getAmount() {
        return amount;
    }

    public void setAmount(String amount) {
        this.amount = amount;
    }

    public BigDecimal getNormalizedAmount() {
        return normalizedAmount;
    }

    public void setNormalizedAmount(BigDecimal normalizedAmount) {
        this.normalizedAmount = normalizedAmount;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public void setIngredient(Ingredient ingredient) {
        this.ingredient = ingredient;
    }

    public Unit getUnit() {
        return unit;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }

    public Recipe getRecipe() {
        return recipe;
    }

    public void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }
}
