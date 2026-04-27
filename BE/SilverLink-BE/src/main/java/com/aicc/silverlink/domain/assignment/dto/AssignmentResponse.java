package com.aicc.silverlink.domain.assignment.dto;

import com.aicc.silverlink.domain.assignment.entity.Assignment;
 // 👈 엔티티 안에 있는 Enum 가져오기
import com.aicc.silverlink.domain.assignment.entity.AssignmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AssignmentResponse {
    private Long assignmentId;

    // 상담사 정보
    private Long counselorId;
    private String counselorName;

    // 어르신 정보
    private Long elderlyId;
    private String elderlyName;

    // 배정 상세 정보
    private String assignedByAdminName; // 배정한 관리자 이름
    private AssignmentStatus status;    // 상태 (ACTIVE / ENDED)
    private LocalDateTime assignedAt;   // 시작일
    private LocalDateTime endedAt;      // 종료일

    // 엔티티 -> DTO 변환 메서드 (Factory Method)
    public static AssignmentResponse from(Assignment assignment) {
        return AssignmentResponse.builder()
                .assignmentId(assignment.getId())
                .counselorId(assignment.getCounselor().getId())
                .counselorName(assignment.getCounselor().getUser().getName()) // User 테이블의 이름 가져오기
                .elderlyId(assignment.getElderly().getId())
                .elderlyName(assignment.getElderly().getUser().getName())     // User 테이블의 이름 가져오기
                .assignedByAdminName(assignment.getAssignedBy().getUser().getName()) // 관리자 이름
                .status(assignment.getStatus())
                .assignedAt(assignment.getAssignedAt())
                .endedAt(assignment.getEndedAt())
                .build();
    }
}