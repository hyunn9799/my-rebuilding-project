package com.aicc.silverlink.domain.inquiry.dto;

import com.aicc.silverlink.domain.inquiry.entity.Inquiry;
import com.aicc.silverlink.domain.inquiry.entity.Inquiry.InquiryStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class InquiryResponse {
    private Long id;
    private String title;
    private String questionText;
    private InquiryStatus status;
    private LocalDateTime createdAt;
    private String answerText;
    private LocalDateTime answeredAt;
    private String elderlyName;

    public static InquiryResponse from(Inquiry inquiry, String answerText, LocalDateTime answeredAt) {
        return InquiryResponse.builder()
                .id(inquiry.getId())
                .title(inquiry.getTitle())
                .questionText(inquiry.getQuestionText())
                .status(inquiry.getStatus())
                .createdAt(inquiry.getCreatedAt())
                .answerText(answerText)
                .answeredAt(answeredAt)
                .elderlyName(inquiry.getElderly().getUser().getName())
                .build();
    }
}
