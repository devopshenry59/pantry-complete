package org.liftoff.thepantry.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.liftoff.thepantry.data.FavoriteRepository;
import org.liftoff.thepantry.data.UnitRepository;
import org.liftoff.thepantry.models.Unit;
import org.liftoff.thepantry.services.RecipeImageStorageService;
import org.liftoff.thepantry.services.QuantityNormalizationService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminRecipeControllerTests {

    @Mock
    private UnitRepository unitRepository;

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private RecipeImageStorageService recipeImageStorageService;

    @Mock
    private QuantityNormalizationService quantityNormalizationService;

    @InjectMocks
    private AdminRecipeController controller;

    @Test
    void newUnitSavesUniqueUnitAndReturnsToRecipe() {
        Unit unit = new Unit("tablespoon");
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(unit, "unit");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        when(unitRepository.findByName("tablespoon")).thenReturn(Collections.emptyList());

        String view = controller.newUnit(unit, errors, 12, redirectAttributes);

        assertEquals("redirect:12#message", view);
        verify(unitRepository).save(unit);
    }

    @Test
    void newUnitRejectsDuplicateName() {
        Unit unit = new Unit("cup");
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(unit, "unit");
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        when(unitRepository.findByName("cup")).thenReturn(Collections.singletonList(new Unit("cup")));

        String view = controller.newUnit(unit, errors, 12, redirectAttributes);

        assertEquals("redirect:12#message", view);
        verify(unitRepository, never()).save(unit);
    }
}
