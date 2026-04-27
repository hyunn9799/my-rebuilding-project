package com.aicc.silverlink.domain.inquiry.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class InquiryRequest {
    private String title;
    private String questionText;
    // For answer
    private String answerText;
}
