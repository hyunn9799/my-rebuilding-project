package com.aicc.silverlink.domain.complaint.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ComplaintAnswerDto {

    @NotBlank(message = "답변 내용은 필수입니다")
    private String replyContent;
}
