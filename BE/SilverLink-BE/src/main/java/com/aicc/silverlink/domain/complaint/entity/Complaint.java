package com.aicc.silverlink.domain.complaint.entity;

import com.aicc.silverlink.domain.admin.entity.Admin;
import com.aicc.silverlink.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "complaints")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Complaint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "complaint_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "writer_user_id", nullable = false)
    private User writer;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ComplaintStatus status;

    @Column(name = "reply_content", columnDefinition = "TEXT")
    private String replyContent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "replied_by_admin_id")
    private Admin repliedBy;

    @Column(name = "replied_at")
    private LocalDateTime repliedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Complaint(User writer, String title, String content) {
        this.writer = writer;
        this.title = title;
        this.content = content;
        this.status = ComplaintStatus.WAITING;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = ComplaintStatus.WAITING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 민원 답변 작성
     */
    public void reply(String replyContent, Admin admin) {
        this.replyContent = replyContent;
        this.repliedBy = admin;
        this.repliedAt = LocalDateTime.now();
        this.status = ComplaintStatus.RESOLVED;
    }

    /**
     * 민원 상태 변경
     */
    public void updateStatus(ComplaintStatus newStatus) {
        this.status = newStatus;
    }

    public enum ComplaintStatus {
        WAITING, PROCESSING, RESOLVED, REJECTED
    }
}
