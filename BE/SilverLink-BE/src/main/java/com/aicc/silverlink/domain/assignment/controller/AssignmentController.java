package com.aicc.silverlink.domain.assignment.controller;

import com.aicc.silverlink.domain.assignment.dto.AssignmentRequest;
import com.aicc.silverlink.domain.assignment.dto.AssignmentResponse;
import com.aicc.silverlink.domain.assignment.service.AssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "상담사 배정", description = "상담사-어르신 배정 관리 API")
@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    // 상담사-노인 배정
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AssignmentResponse> assignCounselor(
            @RequestBody AssignmentRequest request,
            @AuthenticationPrincipal Long currentUserId) {
        AssignmentResponse response = assignmentService.assignCounselor(request, currentUserId);
        return ResponseEntity.created(URI.create("/api/assignments/elderly/" + response.getElderlyId())).body(response);
    }

    // 상담사-노인 배정 해제
    @PostMapping("/unassign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> unassignCounselor(
            @RequestParam Long counselorId,
            @RequestParam Long elderlyId,
            @AuthenticationPrincipal Long currentUserId) {
        assignmentService.unassignCounselor(counselorId, elderlyId, currentUserId);
        return ResponseEntity.ok().build();
    }

    // 상담사 본인에게 배정된 노인 현황
    @GetMapping("/counselor/me")
    @PreAuthorize("hasRole('COUNSELOR')")
    public ResponseEntity<List<AssignmentResponse>> getAssignmentByCounselor(
            @AuthenticationPrincipal Long currentUserId) {
        return ResponseEntity.ok(assignmentService.getAssignmentsByCounselor(currentUserId));
    }

    // 상담사의 배정현황
    @GetMapping("/admin/counselors/{counselorId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AssignmentResponse>> getCounselorAssignmentByAdmin(@PathVariable Long counselorId) {
        return ResponseEntity.ok(assignmentService.getAssignmentsByCounselor(counselorId));
    }

    // 노인의 배정현황
    @GetMapping("/admin/elderly/{elderlyId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AssignmentResponse> getElderlyAssignmentByAdmin(@PathVariable Long elderlyId) {
        AssignmentResponse assignment = assignmentService.getAssignmentByElderlyOrNull(elderlyId);
        if (assignment == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(assignment);
    }
}
