package com.aicc.silverlink.domain.counseling.controller;

import com.aicc.silverlink.domain.counseling.dto.CounselingRecordRequest;
import com.aicc.silverlink.domain.counseling.dto.CounselingRecordResponse;
import com.aicc.silverlink.domain.counseling.service.CounselingRecordService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Tag(name = "상담 기록", description = "상담 기록 관리 API")
@RestController
@RequestMapping("/api/counseling-records")
@RequiredArgsConstructor
public class CounselingRecordController {

    private final CounselingRecordService counselingRecordService;

    @PostMapping
    @PreAuthorize("hasRole('COUNSELOR')")
    public ResponseEntity<CounselingRecordResponse> createRecord(
            @AuthenticationPrincipal Long counselorId,
            @RequestBody @Valid CounselingRecordRequest request) {
        CounselingRecordResponse response = counselingRecordService.createRecord(counselorId, request);
        return ResponseEntity.created(URI.create("/api/counseling-records/" + response.getId())).body(response);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('COUNSELOR')")
    public ResponseEntity<List<CounselingRecordResponse>> getMyRecords(@AuthenticationPrincipal Long counselorId) {
        return ResponseEntity.ok(counselingRecordService.getMyRecords(counselorId));
    }

    @GetMapping("/elderly/{elderlyId}")
    @PreAuthorize("hasAnyRole('COUNSELOR', 'ADMIN', 'GUARDIAN')")
    public ResponseEntity<List<CounselingRecordResponse>> getRecordsByElderly(@PathVariable Long elderlyId) {
        // TODO: Add security check to ensure guardian/counselor is assigned to this
        // elderly
        return ResponseEntity.ok(counselingRecordService.getRecordsByElderly(elderlyId));
    }

    @PutMapping("/{recordId}")
    @PreAuthorize("hasRole('COUNSELOR')")
    public ResponseEntity<CounselingRecordResponse> updateRecord(
            @AuthenticationPrincipal Long counselorId,
            @PathVariable Long recordId,
            @RequestBody @Valid CounselingRecordRequest request) {
        return ResponseEntity.ok(counselingRecordService.updateRecord(counselorId, recordId, request));
    }
}
