package com.aicc.silverlink.domain.notice.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class NoticeReadLogId implements Serializable {
    private Long notice;
    private Long user;
}
