package com.kangaroo.sparring.domain.recommendation.entity;

import com.kangaroo.sparring.domain.recommendation.type.RecommendationType;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "recommendation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Recommendation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recommendation_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecommendationType type;

    @Column(name = "filter_duration", length = 20)
    private String filterDuration;

    @Column(name = "filter_intensity", length = 20)
    private String filterIntensity;

    @Column(name = "filter_location", length = 20)
    private String filterLocation;

    @Builder
    private Recommendation(User user, RecommendationType type, String filterDuration,
                           String filterIntensity, String filterLocation) {
        this.user = user;
        this.type = type;
        this.filterDuration = filterDuration;
        this.filterIntensity = filterIntensity;
        this.filterLocation = filterLocation;
    }

    public static Recommendation createExerciseRecommendation(User user, String duration,
                                                              String intensity, String location) {
        return Recommendation.builder()
                .user(user)
                .type(RecommendationType.EXERCISE)
                .filterDuration(duration)
                .filterIntensity(intensity)
                .filterLocation(location)
                .build();
    }

    public static Recommendation createSupplementRecommendation(User user) {
        return Recommendation.builder()
                .user(user)
                .type(RecommendationType.SUPPLEMENT)
                .build();
    }

}
