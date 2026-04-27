package com.aicc.silverlink.domain.assignment.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AssignmentRequest {
    private Long counselorId;


    private Long elderlyId;


    private Long adminId;
}