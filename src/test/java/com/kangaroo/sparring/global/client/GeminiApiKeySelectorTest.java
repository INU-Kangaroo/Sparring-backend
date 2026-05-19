package com.kangaroo.sparring.global.client;

import com.kangaroo.sparring.global.config.GeminiApiProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GeminiApiKeySelectorTest {

    @Test
    void 키_풀을_라운드로빈으로_순환한다() {
        GeminiApiProperties properties = new GeminiApiProperties();
        properties.setUrl("https://example.com");
        properties.setKeys(List.of("k1", "k2", "k3"));

        GeminiApiKeySelector selector = new GeminiApiKeySelector(properties);

        assertThat(selector.nextKeyOrder()).containsExactly("k1", "k2", "k3");
        assertThat(selector.nextKeyOrder()).containsExactly("k2", "k3", "k1");
        assertThat(selector.nextKeyOrder()).containsExactly("k3", "k1", "k2");
    }

    @Test
    void 복수키에서_중복과_공백을_제거한다() {
        GeminiApiProperties properties = new GeminiApiProperties();
        properties.setUrl("https://example.com");
        properties.setKeys(List.of("k1", "k2", " ", "k1", "k3"));

        GeminiApiKeySelector selector = new GeminiApiKeySelector(properties);

        assertThat(selector.nextKeyOrder()).containsExactly("k1", "k2", "k3");
    }
}
