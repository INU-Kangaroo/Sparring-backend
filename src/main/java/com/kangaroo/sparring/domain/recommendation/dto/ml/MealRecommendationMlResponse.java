package com.kangaroo.sparring.domain.recommendation.dto.ml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MealRecommendationMlResponse(
        @JsonProperty("meal_type") String mealType,
        @JsonProperty("target") Target target,
        @JsonProperty("recommendations") List<Recommendation> recommendations
) {
    public double mealTargetKcalOrZero() {
        return target != null && target.mealTargetKcal() != null ? target.mealTargetKcal() : 0d;
    }

    public List<Recommendation> recommendationsOrEmpty() {
        return recommendations == null ? List.of() : recommendations;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Target(
            @JsonProperty("meal_target_kcal") Double mealTargetKcal
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Recommendation(
            @JsonProperty("rank") Integer rank,
            @JsonProperty("title") String title,
            @JsonProperty("total_kcal") Double totalKcal,
            @JsonProperty("total_carbs") Double totalCarbs,
            @JsonProperty("total_protein") Double totalProtein,
            @JsonProperty("total_fat") Double totalFat,
            @JsonProperty("total_sodium") Double totalSodium,
            @JsonProperty("reasons") List<String> reasons,
            @JsonProperty("recipes") List<Recipe> recipes
    ) {
        public int rankOrZero() {
            return rank == null ? 0 : rank;
        }

        public String titleOrEmpty() {
            return title == null ? "" : title;
        }

        public double totalKcalOrZero() {
            return totalKcal == null ? 0d : totalKcal;
        }

        public double totalCarbsOrZero() {
            return totalCarbs == null ? 0d : totalCarbs;
        }

        public double totalProteinOrZero() {
            return totalProtein == null ? 0d : totalProtein;
        }

        public double totalFatOrZero() {
            return totalFat == null ? 0d : totalFat;
        }

        public List<String> reasonsOrEmpty() {
            return reasons == null ? List.of() : reasons;
        }

        public List<Recipe> recipesOrEmpty() {
            return recipes == null ? List.of() : recipes;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Recipe(
            @JsonProperty("recipe_id") Long recipeId,
            @JsonProperty("recipe_name") String recipeName,
            @JsonProperty("kcal") Double kcal,
            @JsonProperty("carbs") Double carbs,
            @JsonProperty("protein") Double protein,
            @JsonProperty("fat") Double fat,
            @JsonProperty("sodium") Double sodium
    ) {
        public long recipeIdOrZero() {
            return recipeId == null ? 0L : recipeId;
        }

        public String recipeNameOrEmpty() {
            return recipeName == null ? "" : recipeName;
        }

        public double kcalOrZero() {
            return kcal == null ? 0d : kcal;
        }

        public double carbsOrZero() {
            return carbs == null ? 0d : carbs;
        }

        public double proteinOrZero() {
            return protein == null ? 0d : protein;
        }

        public double fatOrZero() {
            return fat == null ? 0d : fat;
        }
    }
}
