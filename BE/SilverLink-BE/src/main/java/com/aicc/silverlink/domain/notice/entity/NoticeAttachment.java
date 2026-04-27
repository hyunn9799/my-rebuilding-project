package com.aicc.silverlink.domain.notice.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "notice_attachments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoticeAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notice_attachment_id")
    private Long id;

    // Notice와의 관계는 유지 (어떤 공지사항의 파일인지 알아야 하므로)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notice_id", nullable = false)
    private Notice notice;

    /* * [수정] FileAttachment 관계 삭제 -> 직접 컬럼 매핑
     */

    @Column(name = "file_name", nullable = false)
    private String fileName;         // 저장된 파일명 (UUID 등)

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName; // 사용자가 올린 원본 파일명

    @Column(name = "file_path", nullable = false)
    private String filePath;         // S3 Key 또는 저장 경로

    @Column(name = "file_size")
    private Long fileSize;           // 파일 크기 (선택사항)

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // 객체 생성을 위한 빌더 패턴 추가
    @Builder
    public NoticeAttachment(Notice notice, String fileName, String originalFileName, String filePath, Long fileSize) {
        this.notice = notice;
        this.fileName = fileName;
        this.originalFileName = originalFileName;
        this.filePath = filePath;
        this.fileSize = fileSize;
    }

    // 연관관계 편의 메서드 (Notice 쪽 리스트에도 넣어주기 위함 - 선택사항)
    public void setNotice(Notice notice) {
        this.notice = notice;
        // notice.getAttachments().add(this); // Notice 엔티티 구조에 따라 필요시 주석 해제
    }
}