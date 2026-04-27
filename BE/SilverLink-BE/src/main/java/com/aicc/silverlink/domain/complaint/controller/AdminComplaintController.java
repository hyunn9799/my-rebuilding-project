package com.aicc.silverlink.domain.complaint.controller;

import com.aicc.silverlink.domain.admin.entity.Admin;
import com.aicc.silverlink.domain.complaint.dto.ComplaintAnswerDto;
import com.aicc.silverlink.domain.complaint.dto.ComplaintResponse;
import com.aicc.silverlink.domain.complaint.entity.Complaint.ComplaintStatus;
import com.aicc.silverlink.domain.complaint.service.ComplaintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "민원 관리 (관리자)", description = "민원 처리 API (관리자 전용)")
@RestController
@RequestMapping("/api/admin/complaints")
@RequiredArgsConstructor
public class AdminComplaintController {

    private final ComplaintService complaintService;

    @Operation(summary = "전체 민원 목록 조회", description = "관리자가 전체 민원 목록을 조회합니다")
    @GetMapping
    public ResponseEntity<Page<ComplaintResponse>> getAllComplaints(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(complaintService.getAllComplaints(pageable));
    }

    @Operation(summary = "상태별 민원 조회", description = "특정 상태의 민원만 조회합니다")
    @GetMapping("/status/{status}")
    public ResponseEntity<Page<ComplaintResponse>> getComplaintsByStatus(
            @PathVariable ComplaintStatus status,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(complaintService.getComplaintsByStatus(status, pageable));
    }

    @Operation(summary = "민원 답변", description = "관리자가 민원에 답변합니다")
    @PostMapping("/{id}/reply")
    public ResponseEntity<ComplaintResponse> replyToComplaint(
            @PathVariable Long id,
            @Valid @RequestBody ComplaintAnswerDto answerDto,
            @AuthenticationPrincipal Admin admin) {
        return ResponseEntity.ok(complaintService.replyToComplaint(id, answerDto.getReplyContent(), admin));
    }

    @Operation(summary = "민원 상태 변경", description = "민원 처리 상태를 변경합니다")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ComplaintResponse> updateComplaintStatus(
            @PathVariable Long id,
            @RequestParam ComplaintStatus status) {
        return ResponseEntity.ok(complaintService.updateComplaintStatus(id, status));
    }
}
