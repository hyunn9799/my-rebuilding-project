package com.aicc.silverlink.domain.chatbot.dto;

import com.aicc.silverlink.domain.inquiry.entity.Inquiry;
import com.aicc.silverlink.domain.inquiry.entity.InquiryAnswer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Inquiry 데이터를 Python 챗봇 서비스로 전달하기 위한 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InquiryDataDto {
    private Long inquiryId;
    private Long elderlyUserId;
    private Long guardianUserId;
    private String question;
    private String answer;
    private LocalDateTime createdAt;

    /**
     * Inquiry와 InquiryAnswer를 결합하여 InquiryDataDto로 변환
     */
    public static InquiryDataDto from(Inquiry inquiry, InquiryAnswer inquiryAnswer) {
        return InquiryDataDto.builder()
                .inquiryId(inquiry.getId())
                .elderlyUserId(inquiry.getElderly().getId())
                .guardianUserId(inquiry.getCreatedBy().getId())
                .question(inquiry.getQuestionText())
                .answer(inquiryAnswer != null ? inquiryAnswer.getAnswerText() : null)
                .createdAt(inquiry.getCreatedAt())
                .build();
    }
}
