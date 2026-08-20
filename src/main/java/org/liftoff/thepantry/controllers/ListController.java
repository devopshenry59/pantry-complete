package org.liftoff.thepantry.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.liftoff.thepantry.data.IngredientSearchCriterion;
import org.liftoff.thepantry.data.RecipeIngredientRepository;
import org.liftoff.thepantry.data.RecipeRepository;
import org.liftoff.thepantry.data.SearchDTO;
import org.liftoff.thepantry.data.UnitRepository;
import org.liftoff.thepantry.models.Recipe;
import org.liftoff.thepantry.models.RecipeIngredient;
import org.liftoff.thepantry.models.Unit;
import org.liftoff.thepantry.services.QuantityNormalizationService;
import org.liftoff.thepantry.services.QuantityNormalizationService.NormalizedQuantity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.validation.Valid;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("list")
public class ListController {

    private final RecipeRepository recipeRepository;
    private final RecipeIngredientRepository recipeIngredientRepository;
    private final UnitRepository unitRepository;
    private final QuantityNormalizationService quantityNormalizationService;
    private final ObjectMapper objectMapper;

    public ListController(RecipeRepository recipeRepository,
                          RecipeIngredientRepository recipeIngredientRepository,
                          UnitRepository unitRepository,
                          QuantityNormalizationService quantityNormalizationService,
                          ObjectMapper objectMapper) {
        this.recipeRepository = recipeRepository;
        this.recipeIngredientRepository = recipeIngredientRepository;
        this.unitRepository = unitRepository;
        this.quantityNormalizationService = quantityNormalizationService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("")
    public String index(Model model) {
        model.addAttribute("banner", "search");
        model.addAttribute("recipes", recipeRepository.findAll());
        return "list/index";
    }

    @PostMapping()
    @Transactional(readOnly = true)
    public String searchRecipe(@ModelAttribute @Valid SearchDTO searchDTO, Model model, Errors errors) {
        List<PreparedCriterion> criteria;
        try {
            criteria = prepareCriteria(searchDTO.getCriteria());
        } catch (IllegalArgumentException e) {
            return renderResults(model, new ArrayList<>(), 0, e.getMessage());
        }

        if (criteria.isEmpty()) {
            return renderResults(model, new ArrayList<>(), 0,
                    "Choose at least one ingredient and enter how much you have.");
        }

        List<Integer> ingredientIds = criteria.stream()
                .map(PreparedCriterion::getIngredientId)
                .collect(Collectors.toList());
        List<Recipe> recipes = recipeIngredientRepository.findRecipesContainingAllIngredients(
                        ingredientIds, ingredientIds.size()).stream()
                .filter(recipe -> hasEnoughOfEveryIngredient(recipe, criteria))
                .collect(Collectors.toList());

        return renderResults(model, recipes, criteria.size(), null);
    }

    private String renderResults(Model model, List<Recipe> recipes, int criterionCount, String message) {
        model.addAttribute("banner", "search");
        model.addAttribute("recipes", recipes);
        model.addAttribute("selectedIngredientCount", criterionCount);
        if (message != null) {
            model.addAttribute("message", message);
        }
        return "list/index";
    }

    private List<PreparedCriterion> prepareCriteria(String rawCriteria) {
        if (rawCriteria == null || rawCriteria.isBlank()) {
            return new ArrayList<>();
        }

        List<IngredientSearchCriterion> submittedCriteria;
        try {
            submittedCriteria = objectMapper.readValue(rawCriteria,
                    new TypeReference<List<IngredientSearchCriterion>>() { });
        } catch (IOException e) {
            throw new IllegalArgumentException("The ingredient quantities could not be read. Please try again.");
        }

        Set<Integer> seenIngredientIds = new LinkedHashSet<>();
        List<PreparedCriterion> preparedCriteria = new ArrayList<>();
        for (IngredientSearchCriterion criterion : submittedCriteria) {
            if (criterion.getIngredientId() <= 0 || !seenIngredientIds.add(criterion.getIngredientId())) {
                continue;
            }

            Unit unit = null;
            if (criterion.getUnitId() > 0) {
                unit = unitRepository.findById(criterion.getUnitId())
                        .orElseThrow(() -> new IllegalArgumentException("One selected unit is no longer available."));
            }
            NormalizedQuantity availableQuantity = quantityNormalizationService
                    .normalize(criterion.getAmount(), unit)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Use a numeric amount or fraction with a supported unit."));
            preparedCriteria.add(new PreparedCriterion(criterion.getIngredientId(), availableQuantity));
        }
        return preparedCriteria;
    }

    private boolean hasEnoughOfEveryIngredient(Recipe recipe, List<PreparedCriterion> criteria) {
        for (PreparedCriterion criterion : criteria) {
            List<RecipeIngredient> recipeIngredients = recipeIngredientRepository
                    .findByRecipeIdAndIngredientId(recipe.getId(), criterion.getIngredientId());
            if (recipeIngredients.isEmpty()) {
                return false;
            }

            RecipeIngredient requirement = recipeIngredients.get(0);
            Optional<NormalizedQuantity> normalizedRequirement = quantityNormalizationService
                    .normalize(requirement.getAmount(), requirement.getUnit());
            if (normalizedRequirement.isEmpty()
                    || normalizedRequirement.get().getDimension() != criterion.getAvailableQuantity().getDimension()) {
                return false;
            }

            java.math.BigDecimal requiredAmount = requirement.getNormalizedAmount() == null
                    ? normalizedRequirement.get().getAmount()
                    : requirement.getNormalizedAmount();
            if (criterion.getAvailableQuantity().getAmount().compareTo(requiredAmount) < 0) {
                return false;
            }
        }
        return true;
    }

    private static class PreparedCriterion {
        private final int ingredientId;
        private final NormalizedQuantity availableQuantity;

        private PreparedCriterion(int ingredientId, NormalizedQuantity availableQuantity) {
            this.ingredientId = ingredientId;
            this.availableQuantity = availableQuantity;
        }

        private int getIngredientId() {
            return ingredientId;
        }

        private NormalizedQuantity getAvailableQuantity() {
            return availableQuantity;
        }
    }
}
