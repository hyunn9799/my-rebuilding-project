package com.aicc.silverlink.domain.chatbot.dto;

import com.aicc.silverlink.domain.inquiry.entity.Faq;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * FAQ 데이터를 Python 챗봇 서비스로 전달하기 위한 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaqDataDto {
    private Long faqId;
    private String category;
    private String question;
    private String answerText;
    private LocalDateTime updatedAt;

    /**
     * Faq 엔티티를 FaqDataDto로 변환
     */
    public static FaqDataDto from(Faq faq) {
        return FaqDataDto.builder()
                .faqId(faq.getId())
                .category(faq.getCategory().name())
                .question(faq.getQuestion())
                .answerText(faq.getAnswerText())
                .updatedAt(faq.getUpdatedAt())
                .build();
    }
}
