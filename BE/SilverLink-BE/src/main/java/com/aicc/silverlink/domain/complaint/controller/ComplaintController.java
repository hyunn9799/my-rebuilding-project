package com.aicc.silverlink.domain.complaint.controller;

import com.aicc.silverlink.domain.complaint.dto.ComplaintRequest;
import com.aicc.silverlink.domain.complaint.dto.ComplaintResponse;
import com.aicc.silverlink.domain.complaint.service.ComplaintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "민원", description = "민원 접수/조회 API (보호자)")
@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;

    @Operation(summary = "민원 등록", description = "보호자가 민원을 등록합니다")
    @PostMapping
    public ResponseEntity<ComplaintResponse> createComplaint(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ComplaintRequest request) {
        return ResponseEntity.ok(complaintService.createComplaint(userId, request));
    }

    @Operation(summary = "내 민원 목록 조회", description = "보호자가 자신의 민원 목록을 조회합니다")
    @GetMapping("/my")
    public ResponseEntity<List<ComplaintResponse>> getMyComplaints(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(complaintService.getMyComplaints(userId));
    }

    @Operation(summary = "민원 상세 조회", description = "특정 민원의 상세 정보를 조회합니다")
    @GetMapping("/{id}")
    public ResponseEntity<ComplaintResponse> getComplaintDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(complaintService.getComplaintDetail(id, userId));
    }

    @Operation(summary = "민원 통계 조회", description = "보호자의 민원 상태별 통계를 조회합니다")
    @GetMapping("/my/stats")
    public ResponseEntity<Map<String, Long>> getMyComplaintStats(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(complaintService.getMyComplaintStats(userId));
    }
}
