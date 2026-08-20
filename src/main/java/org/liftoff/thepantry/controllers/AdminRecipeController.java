package org.liftoff.thepantry.controllers;

import org.liftoff.thepantry.data.FavoriteRepository;
import org.liftoff.thepantry.data.IngredientRepository;
import org.liftoff.thepantry.data.RecipeIngredientRepository;
import org.liftoff.thepantry.data.RecipeRepository;
import org.liftoff.thepantry.data.UnitRepository;
import org.liftoff.thepantry.models.Ingredient;
import org.liftoff.thepantry.models.Recipe;
import org.liftoff.thepantry.models.RecipeIngredient;
import org.liftoff.thepantry.models.Unit;
import org.liftoff.thepantry.services.RecipeImageStorageService;
import org.liftoff.thepantry.services.QuantityNormalizationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("admin/recipes")
public class AdminRecipeController {

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private IngredientRepository ingredientRepository;

    @Autowired
    private RecipeIngredientRepository recipeIngredientRepository;

    @Autowired
    private UnitRepository unitRepository;

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private RecipeImageStorageService recipeImageStorageService;

    @Autowired
    private QuantityNormalizationService quantityNormalizationService;

    @GetMapping("")
    public String index(Model model) {
        model.addAttribute("recipes", recipeRepository.findAll(Sort.by(Sort.Direction.ASC, "name")));
        model.addAttribute(new Recipe());
        return "admin/recipes/index";
    }

    @PostMapping("add-recipe")
    public String addRecipe(Model model, @ModelAttribute @Valid Recipe newRecipe, Errors errors, RedirectAttributes ra) {
        if (errors.hasErrors()) {
            model.addAttribute("recipes", recipeRepository.findAll(Sort.by(Sort.Direction.ASC, "name")));
            return "admin/recipes/index";
        }
        if (!recipeRepository.findByName(newRecipe.getName()).isEmpty()) {
            ra.addFlashAttribute("class", "alert alert-danger");
            ra.addFlashAttribute("message", "Recipe '" + newRecipe.getName() + "' already exists.");
            return "redirect:/admin/recipes/";
        }

        recipeRepository.save(newRecipe);
        return "redirect:/admin/recipes/edit/" + newRecipe.getId();
    }

    @PostMapping("delete-recipe")
    public String deleteRecipe(@RequestParam int recipeId, RedirectAttributes ra) {
        Optional<Recipe> optRecipe = recipeRepository.findById(recipeId);
        if (optRecipe.isEmpty()) {
            ra.addFlashAttribute("class", "alert alert-danger");
            ra.addFlashAttribute("message", "Recipe not found.");
            return "redirect:/admin/recipes/";
        }

        Recipe recipe = optRecipe.get();
        favoriteRepository.deleteAll(favoriteRepository.findByRecipe_Id(recipeId));
        List<RecipeIngredient> recipeIngredients = recipeIngredientRepository.findByRecipeId(recipeId);
        recipeIngredientRepository.deleteAll(recipeIngredients);

        try {
            recipeImageStorageService.delete(recipe.getImage());
        } catch (IOException e) {
            ra.addFlashAttribute("class", "alert alert-danger");
            ra.addFlashAttribute("message", "Recipe image could not be deleted.");
            return "redirect:/admin/recipes/";
        }

        recipeRepository.deleteById(recipeId);

        ra.addFlashAttribute("class", "alert alert-success");
        ra.addFlashAttribute("message", "Recipe '" + recipe.getName() + "' deleted successfully");
        return "redirect:/admin/recipes/";
    }

    @GetMapping("edit/{recipeId}")
    public String editRecipe(Model model, @PathVariable int recipeId) {
        Optional<Recipe> optRecipe = recipeRepository.findById(recipeId);
        Recipe recipe = optRecipe.get();
        model.addAttribute("recipe", recipe);
        model.addAttribute("units", unitRepository.findAll(Sort.by(Sort.Direction.ASC, "name")));
        model.addAttribute("ingredients", ingredientRepository.findAll(Sort.by(Sort.Direction.ASC, "name")));
        model.addAttribute("recipeIngredients", recipeIngredientRepository.findByRecipeId(recipeId));
        model.addAttribute(new RecipeIngredient());
        model.addAttribute(new Ingredient());
        model.addAttribute(new Unit());
        return "admin/recipes/edit";
    }

    @PostMapping("edit/save-recipe")
    public String saveRecipe(@ModelAttribute Recipe recipe, @RequestParam int recipeId, Errors errors, RedirectAttributes ra) {
        if (errors.hasErrors()) {
            return "redirect:" + recipeId + "#message";
        }

        Optional<Recipe> optRecipe = recipeRepository.findById(recipeId);
        if (optRecipe.isEmpty()) {
            ra.addFlashAttribute("class", "alert alert-danger");
            ra.addFlashAttribute("message", "Recipe not found.");
            return "redirect:/admin/recipes/";
        }

        Recipe existingRecipe = optRecipe.get();
        existingRecipe.setName(recipe.getName());
        existingRecipe.setDescription(recipe.getDescription());
        existingRecipe.setInstructions(recipe.getInstructions());
        existingRecipe.setImage(recipe.getImage());
        recipeRepository.save(existingRecipe);

        ra.addFlashAttribute("class", "alert alert-success");
        ra.addFlashAttribute("message", "Recipe '" + existingRecipe.getName() + "' updated successfully");
        return "redirect:" + recipeId + "#message";
    }

    @PostMapping("edit/add-ingredient")
    public String addIngredient(@ModelAttribute RecipeIngredient newRecipeIngredient, @RequestParam String amount, @RequestParam int ingredientId, @RequestParam int recipeId, @RequestParam int unitId, RedirectAttributes ra) {
        if (ingredientId == 0) {
            ra.addFlashAttribute("class", "alert alert-danger");
            ra.addFlashAttribute("message", "Ingredient is required.");
            return "redirect:" + recipeId + "#message";
        }
        if (!recipeIngredientRepository.findByRecipeIdAndIngredientId(recipeId, ingredientId).isEmpty()) {
            Optional<Ingredient> optIngredient = ingredientRepository.findById(ingredientId);
            ra.addFlashAttribute("class", "alert alert-danger");
            ra.addFlashAttribute("message", "Recipe ingredient '" + optIngredient.get().getName() + "' already exists.");
            return "redirect:" + recipeId + "#message";
        }

        Optional<Recipe> optRecipe = recipeRepository.findById(recipeId);
        if (optRecipe.isPresent()) {
            newRecipeIngredient.setRecipe(optRecipe.get());
        }
        Optional<Unit> optUnit = unitRepository.findById(unitId);
        optUnit.ifPresent(newRecipeIngredient::setUnit);
        Optional<Ingredient> optIngredient = ingredientRepository.findById(ingredientId);
        optIngredient.ifPresent(newRecipeIngredient::setIngredient);
        newRecipeIngredient.setAmount(amount);
        quantityNormalizationService.normalize(amount, newRecipeIngredient.getUnit())
                .ifPresent(quantity -> newRecipeIngredient.setNormalizedAmount(quantity.getAmount()));

        recipeIngredientRepository.save(newRecipeIngredient);

        ra.addFlashAttribute("class", "alert alert-success");
        ra.addFlashAttribute("message", "Recipe ingredient '" + optIngredient.get().getName() + "' added successfully.");
        return "redirect:" + recipeId + "#message";
    }

    @PostMapping("edit/edit-ingredient")
    public String editIngredient(@ModelAttribute RecipeIngredient recipeIngredient, @RequestParam int recipeIngredientId, @RequestParam String amount, @RequestParam int ingredientId, @RequestParam int recipeId, @RequestParam int unitId, RedirectAttributes ra) {
        if (ingredientId == 0) {
            ra.addFlashAttribute("class", "alert alert-danger");
            ra.addFlashAttribute("message", "Ingredient is required.");
            return "redirect:" + recipeId + "#message";
        }
        if (!recipeIngredientRepository.findByRecipeIdAndIngredientId(recipeId, ingredientId).isEmpty() && recipeIngredientRepository.findByIdAndIngredientId(recipeIngredientId, ingredientId).isEmpty()) {
            Optional<Ingredient> optIngredient = ingredientRepository.findById(ingredientId);
            ra.addFlashAttribute("class", "alert alert-danger");
            ra.addFlashAttribute("message", "Recipe ingredient '" + optIngredient.get().getName() + "' already exists.");
            return "redirect:" + recipeId + "#message";
        }

        Optional<Recipe> optRecipe = recipeRepository.findById(recipeId);
        optRecipe.ifPresent(recipeIngredient::setRecipe);
        Optional<Unit> optUnit = unitRepository.findById(unitId);
        optUnit.ifPresent(recipeIngredient::setUnit);
        Optional<Ingredient> optIngredient = ingredientRepository.findById(ingredientId);
        optIngredient.ifPresent(recipeIngredient::setIngredient);
        recipeIngredient.setAmount(amount);
        quantityNormalizationService.normalize(amount, recipeIngredient.getUnit())
                .ifPresent(quantity -> recipeIngredient.setNormalizedAmount(quantity.getAmount()));
        recipeIngredient.setId(recipeIngredientId);
        recipeIngredientRepository.save(recipeIngredient);

        ra.addFlashAttribute("class", "alert alert-success");
        ra.addFlashAttribute("message", "Recipe ingredient '" + optIngredient.get().getName() + "' saved successfully.");
        return "redirect:" + recipeId + "#message";
    }

    @PostMapping("edit/delete-ingredient")
    public String deleteRecipeIngredient(@RequestParam int recipeId, @RequestParam int recipeIngredientId, RedirectAttributes ra) {
        Optional<RecipeIngredient> optRecipeIngredient = recipeIngredientRepository.findById(recipeIngredientId);
        RecipeIngredient recipeIngredient = optRecipeIngredient.get();
        recipeIngredientRepository.deleteById(recipeIngredientId);

        ra.addFlashAttribute("class", "alert alert-success");
        ra.addFlashAttribute("message", "Recipe ingredient '" + recipeIngredient.getIngredient().getName() + "' deleted successfully");
        return "redirect:" + recipeId + "#message";
    }

    @PostMapping("edit/new-ingredient")
    public String newIngredient(@ModelAttribute @Valid Ingredient newIngredient, Errors errors, @RequestParam int recipeId, RedirectAttributes ra) {
        if (errors.hasErrors()) {
            ra.addFlashAttribute("class", "alert alert-danger");
            ra.addFlashAttribute("message", "Name is required for new ingredient.");
            return "redirect:" + recipeId + "#message";
        }
        if (!ingredientRepository.findByName(newIngredient.getName()).isEmpty()) {
            ra.addFlashAttribute("class", "alert alert-danger");
            ra.addFlashAttribute("message", "Ingredient '" + newIngredient.getName() + "' already exists.");
            return "redirect:" + recipeId + "#message";
        }

        ingredientRepository.save(newIngredient);

        ra.addFlashAttribute("class", "alert alert-success");
        ra.addFlashAttribute("message", "Ingredient '" + newIngredient.getName() + "' added successfully.");
        return "redirect:" + recipeId + "#message";
    }

    @PostMapping("edit/new-unit")
    public String newUnit(@ModelAttribute @Valid Unit newUnit, Errors errors, @RequestParam int recipeId, RedirectAttributes ra) {
        if (errors.hasErrors()) {
            ra.addFlashAttribute("class", "alert alert-danger");
            ra.addFlashAttribute("message", "Name is required for new unit.");
            return "redirect:" + recipeId + "#message";
        }
        if (!unitRepository.findByName(newUnit.getName()).isEmpty()) {
            ra.addFlashAttribute("class", "alert alert-danger");
            ra.addFlashAttribute("message", "Unit '" + newUnit.getName() + "' already exists.");
            return "redirect:" + recipeId + "#message";
        }

        unitRepository.save(newUnit);

        ra.addFlashAttribute("class", "alert alert-success");
        ra.addFlashAttribute("message", "Unit '" + newUnit.getName() + "' added successfully.");
        return "redirect:" + recipeId + "#message";
    }

    @PostMapping("edit/upload-image")
    public String uploadImage(@RequestParam int recipeId, @RequestParam("file") MultipartFile file, RedirectAttributes ra) {
        Optional<Recipe> optRecipe = recipeRepository.findById(recipeId);
        if (optRecipe.isEmpty()) {
            ra.addFlashAttribute("class", "alert alert-danger");
            ra.addFlashAttribute("message", "Recipe not found.");
            return "redirect:/admin/recipes/";
        }

        Recipe recipe = optRecipe.get();
        try {
            String fileName = recipeImageStorageService.store(file);
            recipe.setImage(fileName);
            recipeRepository.save(recipe);
            ra.addFlashAttribute("class", "alert alert-success");
            ra.addFlashAttribute("message", "Image uploaded successfully.");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("class", "alert alert-danger");
            ra.addFlashAttribute("message", e.getMessage());
        } catch (IOException e) {
            ra.addFlashAttribute("class", "alert alert-danger");
            ra.addFlashAttribute("message", "There was an issue with processing image.");
        }
        return "redirect:" + recipeId + "#message";
    }

    @PostMapping("edit/delete-image")
    public String deleteImage(@RequestParam int recipeId, RedirectAttributes ra) {
        Optional<Recipe> optRecipe = recipeRepository.findById(recipeId);
        if (optRecipe.isEmpty()) {
            ra.addFlashAttribute("class", "alert alert-danger");
            ra.addFlashAttribute("message", "Recipe not found.");
            return "redirect:/admin/recipes/";
        }

        Recipe recipe = optRecipe.get();
        try {
            recipeImageStorageService.delete(recipe.getImage());
            recipe.setImage(null);
            recipeRepository.save(recipe);
            ra.addFlashAttribute("class", "alert alert-success");
            ra.addFlashAttribute("message", "Image deleted successfully.");
        } catch (Exception e) {
            ra.addFlashAttribute("class", "alert alert-danger");
            ra.addFlashAttribute("message", "Image delete failed.");
        }

        return "redirect:" + recipeId + "#message";
    }
}
