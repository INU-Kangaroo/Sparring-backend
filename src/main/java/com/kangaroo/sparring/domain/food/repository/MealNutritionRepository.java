package com.kangaroo.sparring.domain.food.repository;

import com.kangaroo.sparring.domain.food.entity.MealNutrition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MealNutritionRepository extends JpaRepository<MealNutrition, Long> {
}
