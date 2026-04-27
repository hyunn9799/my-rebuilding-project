package com.aicc.silverlink.domain.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Python 챗봇에서 반환되는 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {
    /**
     * 챗봇 답변
     */
    private String answer;

    /**
     * Thread ID (대화 세션 식별자)
     */
    private String threadId;

    /**
     * 참고한 소스 목록 (FAQ ID 또는 Inquiry ID)
     */
    private List<String> sources;

    /**
     * 신뢰도 점수 (0.0 ~ 1.0)
     */
    private Double confidence;
}
