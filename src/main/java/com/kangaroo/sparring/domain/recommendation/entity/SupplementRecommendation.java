package com.kangaroo.sparring.domain.recommendation.entity;

import com.kangaroo.sparring.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "supplement_recommendation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SupplementRecommendation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recommendation_id", nullable = false)
    private Recommendation recommendation;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String dosage;

    @Column(length = 50)
    private String frequency;

    @Column(columnDefinition = "TEXT")
    private String benefits;

    @Column(columnDefinition = "TEXT")
    private String precautions;

    @Builder
    private SupplementRecommendation(Recommendation recommendation, String name, String dosage,
                                   String frequency, String benefits, String precautions) {
        this.recommendation = recommendation;
        this.name = name;
        this.dosage = dosage;
        this.frequency = frequency;
        this.benefits = benefits;
        this.precautions = precautions;
    }

    public static SupplementRecommendation of(Recommendation recommendation, String name, String dosage,
                                            String frequency, String benefits, String precautions) {
        return SupplementRecommendation.builder()
                .recommendation(recommendation)
                .name(name)
                .dosage(dosage)
                .frequency(frequency)
                .benefits(benefits)
                .precautions(precautions)
                .build();
    }
}
