package com.kangaroo.sparring.domain.chatbot.session;

import com.kangaroo.sparring.domain.chatbot.type.MessageRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    private MessageRole role;
    private String content;
    private LocalDateTime timestamp;
}
