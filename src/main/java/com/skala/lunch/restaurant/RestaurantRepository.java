package com.skala.lunch.restaurant;

import com.skala.lunch.restaurant.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
    Optional<Restaurant> findByName(String name);
    boolean existsByName(String name);
    boolean existsByNameAndIdNot(String name, Long id);
    List<Restaurant> findByActiveTrue();
    List<Restaurant> findByCategoryAndActiveTrue(Restaurant.Category category);
    List<Restaurant> findByWalkMinutesLessThanEqualAndActiveTrue(Integer walkMinutes);
}
