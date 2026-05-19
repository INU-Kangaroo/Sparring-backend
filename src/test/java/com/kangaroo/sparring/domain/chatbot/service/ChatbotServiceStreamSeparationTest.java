package com.kangaroo.sparring.domain.chatbot.service;

import com.kangaroo.sparring.domain.chatbot.client.GeminiStreamingClient;
import com.kangaroo.sparring.domain.chatbot.dto.req.ChatMessageRequest;
import com.kangaroo.sparring.domain.chatbot.dto.res.ChatMessageAcceptedResponse;
import com.kangaroo.sparring.domain.chatbot.entity.ChatMessage;
import com.kangaroo.sparring.domain.chatbot.entity.ChatSession;
import com.kangaroo.sparring.domain.chatbot.repository.ChatSessionRepository;
import com.kangaroo.sparring.domain.chatbot.type.MessageRole;
import com.kangaroo.sparring.domain.healthprofile.repository.HealthProfileRepository;
import com.kangaroo.sparring.domain.record.common.RecordReadService;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatbotServiceStreamSeparationTest {

    @Mock
    private ChatSessionRepository sessionRepository;
    @Mock
    private GeminiStreamingClient geminiStreamingClient;
    @Mock
    private HealthProfileRepository healthProfileRepository;
    @Mock
    private RecordReadService recordReadService;
    @Mock
    private Clock kstClock;

    @InjectMocks
    private ChatbotService chatbotService;

    @Test
    void 메시지_생성시_messageId와_streamUrl을_반환하고_사용자메시지를_저장한다() {
        Long userId = 1L;
        String sessionId = "s-1";
        ChatSession session = baseSession(userId, sessionId);

        when(kstClock.instant()).thenReturn(Instant.parse("2026-05-19T01:00:00Z"));
        when(kstClock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
        when(sessionRepository.findById(userId, sessionId)).thenReturn(Optional.of(session));

        ChatMessageAcceptedResponse response = chatbotService.createMessage(userId, sessionId, new ChatMessageRequest("안녕하세요"));

        assertThat(response.getMessageId()).isNotBlank();
        assertThat(response.getStreamUrl()).isEqualTo("/api/chatbot/streams/" + response.getMessageId());

        ArgumentCaptor<ChatSession> captor = ArgumentCaptor.forClass(ChatSession.class);
        verify(sessionRepository).save(captor.capture());
        ChatSession saved = captor.getValue();
        assertThat(saved.getMessages()).hasSize(1);
        assertThat(saved.getMessages().get(0).getRole()).isEqualTo(MessageRole.USER);
        assertThat(saved.getMessages().get(0).getContent()).isEqualTo("안녕하세요");
    }

    @Test
    void 등록된_messageId로_스트림_조회시_Gemini에_세션메시지스냅샷을_전달한다() {
        Long userId = 1L;
        String sessionId = "s-2";
        ChatSession session = baseSession(userId, sessionId);

        when(kstClock.instant()).thenReturn(Instant.parse("2026-05-19T01:00:00Z"));
        when(kstClock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
        when(sessionRepository.findById(userId, sessionId)).thenReturn(Optional.of(session));
        when(healthProfileRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(recordReadService.getRecentBloodPressureRecords(userId, 3)).thenReturn(List.of());
        when(recordReadService.getRecentBloodSugarRecords(userId, 3)).thenReturn(List.of());
        when(geminiStreamingClient.streamChat(anyList(), anyString())).thenReturn(Flux.just("반", "가워요"));

        ChatMessageAcceptedResponse response = chatbotService.createMessage(userId, sessionId, new ChatMessageRequest("인사해줘"));
        chatbotService.streamByMessageId(userId, response.getMessageId());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ChatMessage>> historyCaptor = (ArgumentCaptor<List<ChatMessage>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(List.class);
        verify(geminiStreamingClient).streamChat(historyCaptor.capture(), anyString());
        assertThat(historyCaptor.getValue()).hasSize(1);
        assertThat(historyCaptor.getValue().get(0).getRole()).isEqualTo(MessageRole.USER);
        assertThat(historyCaptor.getValue().get(0).getContent()).isEqualTo("인사해줘");
    }

    @Test
    void 다른_사용자의_messageId는_조회할수없다() {
        Long ownerId = 1L;
        Long otherUserId = 2L;
        String sessionId = "s-3";
        ChatSession session = baseSession(ownerId, sessionId);

        when(kstClock.instant()).thenReturn(Instant.parse("2026-05-19T01:00:00Z"));
        when(kstClock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
        when(sessionRepository.findById(ownerId, sessionId)).thenReturn(Optional.of(session));

        ChatMessageAcceptedResponse response = chatbotService.createMessage(ownerId, sessionId, new ChatMessageRequest("보안"));

        assertThatThrownBy(() -> chatbotService.streamByMessageId(otherUserId, response.getMessageId()))
                .isInstanceOf(CustomException.class)
                .extracting(e -> ((CustomException) e).getErrorCode())
                .isEqualTo(ErrorCode.CHATBOT_STREAM_NOT_FOUND);
    }

    private ChatSession baseSession(Long userId, String sessionId) {
        return ChatSession.builder()
                .sessionId(sessionId)
                .userId(userId)
                .title("새 대화")
                .messages(new ArrayList<>())
                .createdAt(LocalDateTime.of(2026, 5, 19, 10, 0))
                .lastActiveAt(LocalDateTime.of(2026, 5, 19, 10, 0))
                .build();
    }
}
