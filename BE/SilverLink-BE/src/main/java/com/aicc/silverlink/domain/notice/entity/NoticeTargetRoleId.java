package com.aicc.silverlink.domain.notice.entity;

import com.aicc.silverlink.domain.user.entity.Role;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class NoticeTargetRoleId implements Serializable {
    private Long notice;
    private Role targetRole;
}
