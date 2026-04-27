package com.aicc.silverlink.domain.notice.entity;

import com.aicc.silverlink.domain.user.entity.Role;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notice_target_roles")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(NoticeTargetRoleId.class)
public class NoticeTargetRole {
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notice_id", nullable = false)
    private Notice notice;

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "target_role", nullable = false)
    private Role targetRole;
}
