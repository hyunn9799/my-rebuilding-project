package com.aicc.silverlink.domain.notice.dto;

import com.aicc.silverlink.domain.notice.entity.Notice;
import com.aicc.silverlink.domain.notice.entity.NoticeAttachment;
// Notice 클래스 안에 있는 것을 꺼내 쓴다고 명시
import com.aicc.silverlink.domain.notice.entity.Notice.NoticeStatus;
import com.aicc.silverlink.domain.notice.entity.Notice.TargetMode;
import com.aicc.silverlink.domain.notice.entity.NoticeCategory;
import com.aicc.silverlink.domain.user.entity.Role;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
public class NoticeResponse {

    private Long id;
    private String title;
    private String content;
    private String createdByAdminName; // 작성자 이름

    private NoticeCategory category; // 카테고리 추가

    private TargetMode targetMode;
    @Builder.Default
    private List<Role> targetRoles = new ArrayList<>(); // 설정된 타겟 권한들

    @JsonProperty("isPriority")
    private boolean isPriority;
    
    @JsonProperty("isPopup")
    private boolean isPopup;
    private LocalDateTime popupStartAt;
    private LocalDateTime popupEndAt;

    private NoticeStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Req 66: 첨부파일 목록
    @Builder.Default
    private List<AttachmentResponse> attachments = new ArrayList<>();

    // Req 69: 사용자 조회 시 읽음 여부 확인용
    private boolean isRead;

    // 이전글/다음글 ID (추후 구현)
    private Long prevNoticeId;
    private Long nextNoticeId;

    public static NoticeResponse from(Notice notice, List<Role> roles, List<NoticeAttachment> attachments) {
        return NoticeResponse.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .category(notice.getCategory())
                // .createdByAdminName(notice.getCreatedBy().getName()) // Admin 엔티티 구조에 따라 추가
                .targetMode(notice.getTargetMode())
                .targetRoles(roles != null ? roles : new ArrayList<>())
                .isPriority(notice.isPriority())
                .isPopup(notice.isPopup())
                .popupStartAt(notice.getPopupStartAt())
                .popupEndAt(notice.getPopupEndAt())
                .status(notice.getStatus())
                .createdAt(notice.getCreatedAt())
                .updatedAt(notice.getUpdatedAt())
                .attachments(attachments != null ? attachments.stream().map(AttachmentResponse::from).collect(Collectors.toList()) : new ArrayList<>())
                .build();
    }

    // 읽음 여부 포함용 팩토리 메서드
    public static NoticeResponse from(Notice notice, List<Role> roles, List<NoticeAttachment> attachments, boolean isRead) {
        NoticeResponse response = from(notice, roles, attachments);
        response.setRead(isRead);
        return response;
    }

    @Data
    @Builder
    public static class AttachmentResponse {
        private Long id;
        private String fileName;
        private String originalFileName;
        private String filePath;
        private Long fileSize;

        public static AttachmentResponse from(NoticeAttachment attachment) {
            return AttachmentResponse.builder()
                    .id(attachment.getId())
                    .fileName(attachment.getFileName())
                    .originalFileName(attachment.getOriginalFileName())
                    .filePath(attachment.getFilePath())
                    .fileSize(attachment.getFileSize())
                    .build();
        }
    }
}
