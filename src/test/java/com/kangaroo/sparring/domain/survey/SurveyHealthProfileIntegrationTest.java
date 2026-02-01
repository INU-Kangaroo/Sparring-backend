package com.kangaroo.sparring.domain.survey;

import com.kangaroo.sparring.domain.healthprofile.entity.HealthProfile;
import com.kangaroo.sparring.domain.healthprofile.repository.HealthProfileRepository;
import com.kangaroo.sparring.domain.survey.dto.req.SurveySubmitRequest;
import com.kangaroo.sparring.domain.survey.entity.Question;
import com.kangaroo.sparring.domain.survey.entity.QuestionType;
import com.kangaroo.sparring.domain.survey.entity.Survey;
import com.kangaroo.sparring.domain.survey.entity.SurveyType;
import com.kangaroo.sparring.domain.survey.repository.AnswerRepository;
import com.kangaroo.sparring.domain.survey.repository.QuestionRepository;
import com.kangaroo.sparring.domain.survey.repository.SurveyRepository;
import com.kangaroo.sparring.domain.survey.service.SurveyService;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.user.repository.UserRepository;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@Transactional
class SurveyHealthProfileIntegrationTest {

    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("sparring_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.MySQLDialect");
    }

    @Autowired
    private SurveyService surveyService;

    @Autowired
    private SurveyRepository surveyRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private HealthProfileRepository healthProfileRepository;

    @Test
    void submitSurvey_updatesHealthProfileFields() {
        User user = userRepository.save(User.builder()
                .email("test@example.com")
                .password("password")
                .username("tester")
                .build());

        Survey survey = surveyRepository.save(Survey.builder()
                .surveyType(SurveyType.BASIC)
                .title("기본 설문")
                .description("기본 설문 설명")
                .build());

        Question q1 = Question.builder()
                .survey(survey)
                .questionKey("BASIC_BIRTH_DATE")
                .questionType(QuestionType.TEXT)
                .questionText("생년월일")
                .questionOrder(1)
                .healthProfileField("birthDate")
                .build();

        Question q2 = Question.builder()
                .survey(survey)
                .questionKey("BASIC_GENDER")
                .questionType(QuestionType.SINGLE_CHOICE)
                .questionText("성별")
                .questionOrder(2)
                .healthProfileField("gender")
                .build();

        Question q3 = Question.builder()
                .survey(survey)
                .questionKey("BASIC_HEIGHT")
                .questionType(QuestionType.NUMBER)
                .questionText("키")
                .questionOrder(3)
                .healthProfileField("height")
                .build();

        Question q4 = Question.builder()
                .survey(survey)
                .questionKey("BASIC_WEIGHT")
                .questionType(QuestionType.NUMBER)
                .questionText("몸무게")
                .questionOrder(4)
                .healthProfileField("weight")
                .build();

        questionRepository.saveAll(List.of(q1, q2, q3, q4));

        SurveySubmitRequest request = new SurveySubmitRequest(
                SurveyType.BASIC,
                List.of(
                        new SurveySubmitRequest.AnswerItem("BASIC_BIRTH_DATE", "1990-01-01"),
                        new SurveySubmitRequest.AnswerItem("BASIC_GENDER", "MALE"),
                        new SurveySubmitRequest.AnswerItem("BASIC_HEIGHT", "175.5"),
                        new SurveySubmitRequest.AnswerItem("BASIC_WEIGHT", "70.0")
                )
        );

        surveyService.submitSurvey(user.getId(), request);

        HealthProfile healthProfile = healthProfileRepository.findByUserId(user.getId()).orElseThrow();
        assertThat(healthProfile.getBirthDate()).isEqualTo(LocalDate.of(1990, 1, 1));
        assertThat(healthProfile.getGender().name()).isEqualTo("MALE");
        assertThat(healthProfile.getHeight()).isEqualByComparingTo(new BigDecimal("175.5"));
        assertThat(healthProfile.getWeight()).isEqualByComparingTo(new BigDecimal("70.0"));
        assertThat(healthProfile.getBmi()).isNotNull();
        assertThat(answerRepository.findByUserIdAndSurveyType(user.getId(), SurveyType.BASIC)).hasSize(4);
    }

    @Test
    void submitSurvey_rejectsUnsupportedHealthProfileField() {
        User user = userRepository.save(User.builder()
                .email("test2@example.com")
                .password("password")
                .username("tester2")
                .build());

        Survey survey = surveyRepository.save(Survey.builder()
                .surveyType(SurveyType.BASIC)
                .title("기본 설문")
                .description("기본 설문 설명")
                .build());

        Question question = Question.builder()
                .survey(survey)
                .questionKey("BASIC_UNKNOWN")
                .questionType(QuestionType.TEXT)
                .questionText("알 수 없는 항목")
                .questionOrder(1)
                .healthProfileField("unknownField")
                .build();

        questionRepository.save(question);

        SurveySubmitRequest request = new SurveySubmitRequest(
                SurveyType.BASIC,
                List.of(new SurveySubmitRequest.AnswerItem("BASIC_UNKNOWN", "value"))
        );

        assertThatThrownBy(() -> surveyService.submitSurvey(user.getId(), request))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }
}
