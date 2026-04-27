package com.aicc.silverlink.domain.inquiry.dto;

import com.aicc.silverlink.domain.inquiry.entity.Faq;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FaqResponse {
    private Long id;
    private String category;
    private String question;
    private String answerText;
    private int displayOrder;

    public static FaqResponse from(Faq faq) {
        return FaqResponse.builder()
                .id(faq.getId())
                .category(faq.getCategory().name())
                .question(faq.getQuestion())
                .answerText(faq.getAnswerText())
                .displayOrder(faq.getDisplayOrder())
                .build();
    }
}
