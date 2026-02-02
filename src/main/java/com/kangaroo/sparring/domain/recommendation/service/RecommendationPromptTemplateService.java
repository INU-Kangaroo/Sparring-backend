package com.kangaroo.sparring.domain.recommendation.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class RecommendationPromptTemplateService {

    private final String exerciseTemplate;
    private final String supplementTemplate;

    public RecommendationPromptTemplateService() {
        this.exerciseTemplate = readTemplate("prompts/recommendation/exercise_prompt.txt");
        this.supplementTemplate = readTemplate("prompts/recommendation/supplement_prompt.txt");
    }

    public String renderExercisePrompt(Map<String, String> variables) {
        return render(exerciseTemplate, variables);
    }

    public String renderSupplementPrompt(Map<String, String> variables) {
        return render(supplementTemplate, variables);
    }

    private String render(String template, Map<String, String> variables) {
        String rendered = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return rendered;
    }

    private String readTemplate(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("프롬프트 템플릿을 로드할 수 없습니다: " + path, e);
        }
    }
}
