package com.kangaroo.sparring.global.client;

import com.kangaroo.sparring.global.config.GeminiApiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
public class GeminiApiKeySelector {

    private final GeminiApiProperties geminiApiProperties;
    private final AtomicInteger roundRobinCursor = new AtomicInteger(0);

    public String getApiUrl() {
        return geminiApiProperties.getUrl();
    }

    public List<String> nextKeyOrder() {
        List<String> keys = geminiApiProperties.resolvedKeys();
        if (keys.isEmpty()) {
            throw new IllegalStateException("Gemini API key is not configured");
        }

        int keySize = keys.size();
        int start = Math.floorMod(roundRobinCursor.getAndIncrement(), keySize);

        List<String> ordered = new ArrayList<>(keySize);
        for (int i = 0; i < keySize; i++) {
            ordered.add(keys.get((start + i) % keySize));
        }

        return ordered;
    }
}
