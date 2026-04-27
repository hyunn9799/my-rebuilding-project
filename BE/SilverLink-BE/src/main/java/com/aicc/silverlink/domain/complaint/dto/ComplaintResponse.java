package com.aicc.silverlink.domain.complaint.dto;

import com.aicc.silverlink.domain.complaint.entity.Complaint;
import com.aicc.silverlink.domain.complaint.entity.Complaint.ComplaintStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.format.DateTimeFormatter;

@Getter
@Builder
public class ComplaintResponse {
    private Long id;
    private String title;
    private String content;
    private String category;
    private ComplaintStatus status;
    private String createdAt;
    private String response;
    private String respondedAt;
    private String respondedByName;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public static ComplaintResponse from(Complaint complaint) {
        return ComplaintResponse.builder()
                .id(complaint.getId())
                .title(complaint.getTitle())
                .content(complaint.getContent())
                .category(null) // 엔티티에 category 필드 없음 - 추후 추가 가능
                .status(complaint.getStatus())
                .createdAt(complaint.getCreatedAt().format(FORMATTER))
                .response(complaint.getReplyContent())
                .respondedAt(complaint.getRepliedAt() != null ? complaint.getRepliedAt().format(FORMATTER) : null)
                .respondedByName(complaint.getRepliedBy() != null && complaint.getRepliedBy().getUser() != null
                        ? complaint.getRepliedBy().getUser().getName()
                        : null)
                .build();
    }
}
