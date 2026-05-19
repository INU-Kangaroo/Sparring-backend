package com.kangaroo.sparring.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "gemini.api")
public class GeminiApiProperties {

    private List<String> keys = new ArrayList<>();
    private String url;

    public List<String> resolvedKeys() {
        Set<String> deduped = new LinkedHashSet<>();

        if (keys != null) {
            for (String candidate : keys) {
                if (candidate != null && !candidate.isBlank()) {
                    deduped.add(candidate.trim());
                }
            }
        }

        return List.copyOf(deduped);
    }
}
