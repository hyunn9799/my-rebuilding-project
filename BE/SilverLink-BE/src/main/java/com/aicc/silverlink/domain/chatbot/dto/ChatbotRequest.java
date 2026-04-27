package com.aicc.silverlink.domain.chatbot.dto;

import com.fasterxml.jackson.annotation.JsonProperty; // 이 임포트가 필요합니다.
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotRequest {

    private String message;

    @JsonProperty("thread_id") // JSON으로 바뀔 때 thread_id가 됩니다. ⭐
    private String threadId;

    @JsonProperty("guardian_id") // JSON으로 바뀔 때 guardian_id가 됩니다. ⭐
    private Long guardianId;

    @JsonProperty("elderly_id") // JSON으로 바뀔 때 elderly_id가 됩니다. ⭐
    private Long elderlyId;
}