package org.liftoff.thepantry.controllers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.liftoff.thepantry.data.FavoriteRepository;
import org.liftoff.thepantry.data.RecipeRepository;
import org.liftoff.thepantry.models.Favorite;
import org.liftoff.thepantry.models.Recipe;
import org.liftoff.thepantry.models.User;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminFavoriteControllerTests {

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private AuthenticationController authenticationController;

    @InjectMocks
    private AdminFavoriteController controller;

    @Test
    void addSavesFavoriteForSignedInUser() {
        User user = new User("cook", "password");
        user.setId(4);
        Recipe recipe = new Recipe();
        recipe.setId(9);
        MockHttpSession session = new MockHttpSession();
        when(authenticationController.getUserFromSession(session)).thenReturn(user);
        when(recipeRepository.findById(9)).thenReturn(Optional.of(recipe));
        when(favoriteRepository.existsByUser_IdAndRecipe_Id(4, 9)).thenReturn(false);

        String view = controller.add(9, session, new RedirectAttributesModelMap());

        assertEquals("redirect:/recipe/9", view);
        ArgumentCaptor<Favorite> favorite = ArgumentCaptor.forClass(Favorite.class);
        verify(favoriteRepository).save(favorite.capture());
        assertSame(user, favorite.getValue().getUser());
        assertSame(recipe, favorite.getValue().getRecipe());
    }

    @Test
    void addDoesNotCreateDuplicateFavorite() {
        User user = new User("cook", "password");
        user.setId(4);
        Recipe recipe = new Recipe();
        recipe.setId(9);
        MockHttpSession session = new MockHttpSession();
        when(authenticationController.getUserFromSession(session)).thenReturn(user);
        when(recipeRepository.findById(9)).thenReturn(Optional.of(recipe));
        when(favoriteRepository.existsByUser_IdAndRecipe_Id(4, 9)).thenReturn(true);

        String view = controller.add(9, session, new RedirectAttributesModelMap());

        assertEquals("redirect:/recipe/9", view);
        verify(favoriteRepository, never()).save(org.mockito.ArgumentMatchers.any(Favorite.class));
    }
}
