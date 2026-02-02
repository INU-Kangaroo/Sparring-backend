package com.kangaroo.sparring.domain.food.repository;

import com.kangaroo.sparring.domain.food.entity.Food;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FoodRepository extends JpaRepository<Food, Long> {

    /**
     * 활성 음식명 중복 조회 (공백/대소문자 정규화)
     */
    @Query("SELECT f FROM Food f WHERE f.isDeleted = false AND LOWER(TRIM(f.name)) = LOWER(TRIM(:name))")
    Optional<Food> findActiveByNormalizedName(@Param("name") String name);

    /**
     * ID로 조회 (영양 정보 포함)
     */
    @Query("SELECT f FROM Food f LEFT JOIN FETCH f.mealNutrition WHERE f.id = :id AND f.isDeleted = false")
    Optional<Food> findByIdWithNutrition(@Param("id") Long id);

    /**
     * 삭제되지 않은 음식만 조회
     */
    @Query("SELECT f FROM Food f LEFT JOIN FETCH f.mealNutrition WHERE f.isDeleted = false AND LOWER(f.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Food> searchActiveByName(@Param("keyword") String keyword);
}
