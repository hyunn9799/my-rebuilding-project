package com.aicc.silverlink.domain.notice.entity;

import com.aicc.silverlink.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "notice_read_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(NoticeReadLogId.class)
public class NoticeReadLog {
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notice_id", nullable = false)
    private Notice notice;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "read_at", nullable = false)
    private LocalDateTime readAt;

    @PrePersist
    protected void onCreate() {
        if (this.readAt == null) {
            this.readAt = LocalDateTime.now();
        }
    }

    public NoticeReadLog(Notice notice, User user) {
        this.notice = notice;
        this.user = user;
        this.readAt = LocalDateTime.now();
    }
}
