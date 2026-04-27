package com.aicc.silverlink.domain.medication.controller;

import com.aicc.silverlink.domain.medication.dto.MedicationRequest;
import com.aicc.silverlink.domain.medication.dto.MedicationResponse;
import com.aicc.silverlink.domain.medication.service.MedicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "복약", description = "복약 정보 관리 API")
@RestController
@RequestMapping("/api/medications")
@RequiredArgsConstructor
public class MedicationController {

    private final MedicationService medicationService;

    @Operation(summary = "복약 일정 등록", description = "어르신이 복약 일정을 등록합니다")
    @PostMapping
    public ResponseEntity<MedicationResponse> createMedication(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody MedicationRequest request) {
        return ResponseEntity.ok(medicationService.createMedication(userId, request));
    }

    @Operation(summary = "내 복약 일정 목록 조회", description = "어르신의 복약 일정을 조회합니다")
    @GetMapping("/my")
    public ResponseEntity<List<MedicationResponse>> getMyMedications(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(medicationService.getMyMedications(userId));
    }

    @Operation(summary = "복약 일정 상세 조회", description = "특정 복약 일정의 상세 정보를 조회합니다")
    @GetMapping("/{id}")
    public ResponseEntity<MedicationResponse> getMedicationDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(medicationService.getMedicationDetail(id, userId));
    }

    @Operation(summary = "복약 일정 삭제", description = "복약 일정을 비활성화합니다")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMedication(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        medicationService.deleteMedication(id, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "알림 토글", description = "복약 알림을 켜거나 끕니다")
    @PatchMapping("/{id}/reminder")
    public ResponseEntity<MedicationResponse> toggleReminder(
            @PathVariable Long id,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(medicationService.toggleReminder(id, userId));
    }

    @Operation(summary = "상담사: 어르신 복약 일정 조회", description = "상담사가 담당 어르신의 복약 일정을 조회합니다")
    @GetMapping("/elderly/{elderlyId}")
    public ResponseEntity<List<MedicationResponse>> getMedicationsByElderly(
            @PathVariable Long elderlyId) {
        return ResponseEntity.ok(medicationService.getMedicationsByElderly(elderlyId));
    }
}
