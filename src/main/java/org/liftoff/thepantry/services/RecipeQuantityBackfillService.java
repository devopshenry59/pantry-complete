package org.liftoff.thepantry.services;

import org.liftoff.thepantry.data.RecipeIngredientRepository;
import org.liftoff.thepantry.models.RecipeIngredient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class RecipeQuantityBackfillService implements ApplicationRunner {

    private final RecipeIngredientRepository recipeIngredientRepository;
    private final QuantityNormalizationService quantityNormalizationService;

    public RecipeQuantityBackfillService(RecipeIngredientRepository recipeIngredientRepository,
                                         QuantityNormalizationService quantityNormalizationService) {
        this.recipeIngredientRepository = recipeIngredientRepository;
        this.quantityNormalizationService = quantityNormalizationService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (RecipeIngredient recipeIngredient : recipeIngredientRepository.findAll()) {
            recipeIngredient.setNormalizedAmount(
                    quantityNormalizationService.normalize(recipeIngredient.getAmount(), recipeIngredient.getUnit())
                            .map(QuantityNormalizationService.NormalizedQuantity::getAmount)
                            .orElse(null));
        }
    }
}
