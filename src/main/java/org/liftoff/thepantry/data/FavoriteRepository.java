package org.liftoff.thepantry.data;

import org.liftoff.thepantry.models.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Integer> {

    List<Favorite> findByUser_IdOrderByRecipe_NameAsc(int userId);

    List<Favorite> findByRecipe_Id(int recipeId);

    boolean existsByUser_IdAndRecipe_Id(int userId, int recipeId);

    Optional<Favorite> findByUser_IdAndRecipe_Id(int userId, int recipeId);
}
