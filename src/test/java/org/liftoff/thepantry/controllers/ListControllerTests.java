package org.liftoff.thepantry.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.liftoff.thepantry.data.RecipeIngredientRepository;
import org.liftoff.thepantry.data.RecipeRepository;
import org.liftoff.thepantry.data.SearchDTO;
import org.liftoff.thepantry.data.UnitRepository;
import org.liftoff.thepantry.models.Recipe;
import org.liftoff.thepantry.models.RecipeIngredient;
import org.liftoff.thepantry.services.QuantityNormalizationService;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ConcurrentModel;
import org.springframework.validation.BeanPropertyBindingResult;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListControllerTests {

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private RecipeIngredientRepository recipeIngredientRepository;

    @Mock
    private UnitRepository unitRepository;

    private final QuantityNormalizationService quantityNormalizationService =
            new QuantityNormalizationService();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void searchesForRecipesContainingEveryDistinctIngredient() {
        ListController controller = new ListController(recipeRepository, recipeIngredientRepository,
                unitRepository, quantityNormalizationService, objectMapper);
        SearchDTO searchDTO = new SearchDTO();
        searchDTO.setCriteria("[" +
                "{\"ingredientId\":12,\"amount\":\"2\",\"unitId\":0}," +
                "{\"ingredientId\":24,\"amount\":\"2\",\"unitId\":0}," +
                "{\"ingredientId\":12,\"amount\":\"2\",\"unitId\":0}]");
        Recipe guacamole = new Recipe();
        guacamole.setId(55);
        RecipeIngredient avocado = new RecipeIngredient();
        avocado.setAmount("1");
        avocado.setNormalizedAmount(java.math.BigDecimal.ONE.setScale(6));
        RecipeIngredient salt = new RecipeIngredient();
        salt.setAmount("1");
        salt.setNormalizedAmount(java.math.BigDecimal.ONE.setScale(6));
        when(recipeIngredientRepository.findRecipesContainingAllIngredients(
                java.util.Arrays.asList(12, 24), 2)).thenReturn(Collections.singletonList(guacamole));
        when(recipeIngredientRepository.findByRecipeIdAndIngredientId(55, 12))
                .thenReturn(Collections.singletonList(avocado));
        when(recipeIngredientRepository.findByRecipeIdAndIngredientId(55, 24))
                .thenReturn(Collections.singletonList(salt));
        ConcurrentModel model = new ConcurrentModel();

        String view = controller.searchRecipe(searchDTO, model,
                new BeanPropertyBindingResult(searchDTO, "searchDTO"));

        assertEquals("list/index", view);
        assertEquals(2, model.getAttribute("selectedIngredientCount"));
        assertEquals(Collections.singletonList(guacamole), model.getAttribute("recipes"));
        verify(recipeIngredientRepository).findRecipesContainingAllIngredients(
                java.util.Arrays.asList(12, 24), 2);
    }

    @Test
    void emptySelectionReturnsGuidanceWithoutQueryingDatabase() {
        ListController controller = new ListController(recipeRepository, recipeIngredientRepository,
                unitRepository, quantityNormalizationService, objectMapper);
        SearchDTO searchDTO = new SearchDTO();
        ConcurrentModel model = new ConcurrentModel();

        controller.searchRecipe(searchDTO, model,
                new BeanPropertyBindingResult(searchDTO, "searchDTO"));

        assertEquals("Choose at least one ingredient and enter how much you have.", model.getAttribute("message"));
        assertEquals(Collections.emptyList(), model.getAttribute("recipes"));
    }
}
