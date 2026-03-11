package com.kangaroo.sparring.domain.survey.entity;

import com.kangaroo.sparring.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "questions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"survey_id", "question_key", "question_stage"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Question extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_id", nullable = false)
    private Survey survey;

    @Column(name = "question_key", nullable = false, length = 100)
    private String questionKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_stage", nullable = false, length = 20)
    private QuestionStage questionStage;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, length = 20)
    private QuestionType questionType;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "question_order", nullable = false)
    private Integer questionOrder;

    @Column(name = "is_required", nullable = false)
    @Builder.Default
    private Boolean isRequired = true;

    @Column(name = "health_profile_field", length = 100)
    private String healthProfileField;

    @Column(columnDefinition = "TEXT")
    private String options; // JSON 형태로 저장

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Answer> answers = new ArrayList<>();

    void setSurvey(Survey survey) {
        this.survey = survey;
    }
}
