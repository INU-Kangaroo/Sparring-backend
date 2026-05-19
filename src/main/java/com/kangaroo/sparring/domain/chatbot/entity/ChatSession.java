package com.kangaroo.sparring.domain.chatbot.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatSession {

    private String sessionId;
    private Long userId;
    private String title;

    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime lastActiveAt;
}
