package org.liftoff.thepantry.controllers;

import org.liftoff.thepantry.data.FavoriteRepository;
import org.liftoff.thepantry.data.RecipeRepository;
import org.liftoff.thepantry.models.Favorite;
import org.liftoff.thepantry.models.Recipe;
import org.liftoff.thepantry.models.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.util.Optional;

@Controller
@RequestMapping("admin/favorites")
public class AdminFavoriteController {

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private RecipeRepository recipeRepository;

    @Autowired
    private AuthenticationController authenticationController;

    @GetMapping({"", "/"})
    public String index(Model model, HttpSession session) {
        User user = authenticationController.getUserFromSession(session);
        if (user == null) {
            return "redirect:/login";
        }

        model.addAttribute("favorites", favoriteRepository.findByUser_IdOrderByRecipe_NameAsc(user.getId()));
        return "admin/favorites/index";
    }

    @PostMapping("add")
    public String add(@RequestParam int recipeId, HttpSession session, RedirectAttributes ra) {
        User user = authenticationController.getUserFromSession(session);
        if (user == null) {
            return "redirect:/login";
        }

        Optional<Recipe> recipe = recipeRepository.findById(recipeId);
        if (recipe.isEmpty()) {
            ra.addFlashAttribute("class", "alert alert-danger");
            ra.addFlashAttribute("message", "Recipe not found.");
            return "redirect:/recipeList";
        }

        if (!favoriteRepository.existsByUser_IdAndRecipe_Id(user.getId(), recipeId)) {
            favoriteRepository.save(new Favorite(user, recipe.get()));
            ra.addFlashAttribute("class", "alert alert-success");
            ra.addFlashAttribute("message", "Recipe added to favorites.");
        }

        return "redirect:/recipe/" + recipeId;
    }

    @PostMapping("remove")
    public String remove(@RequestParam int recipeId,
                         @RequestParam(defaultValue = "recipe") String returnTo,
                         HttpSession session,
                         RedirectAttributes ra) {
        User user = authenticationController.getUserFromSession(session);
        if (user == null) {
            return "redirect:/login";
        }

        favoriteRepository.findByUser_IdAndRecipe_Id(user.getId(), recipeId)
                .ifPresent(favoriteRepository::delete);
        ra.addFlashAttribute("class", "alert alert-success");
        ra.addFlashAttribute("message", "Recipe removed from favorites.");

        if ("favorites".equals(returnTo)) {
            return "redirect:/admin/favorites/";
        }
        return "redirect:/recipe/" + recipeId;
    }
}
