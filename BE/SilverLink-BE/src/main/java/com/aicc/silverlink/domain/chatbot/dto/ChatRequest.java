package com.aicc.silverlink.domain.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 프론트엔드에서 챗봇으로 전달되는 요청 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequest {
    /**
     * 사용자 질문
     */
    private String message;

    /**
     * 보호자 ID (프론트엔드에서 전달, 성능 최적화)
     */
    private Long guardianId;

    /**
     * 어르신 ID (프론트엔드에서 전달, 성능 최적화)
     */
    private Long elderlyId;

    /**
     * 대화 스레드 ID (선택사항, 없으면 자동 생성)
     */
    private String threadId;
}
