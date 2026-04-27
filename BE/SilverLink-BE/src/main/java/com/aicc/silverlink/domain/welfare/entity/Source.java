package com.aicc.silverlink.domain.welfare.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Source {
    CENTRAL("중앙부처"),
    LOCAL("지자체");

    private final String description;
}