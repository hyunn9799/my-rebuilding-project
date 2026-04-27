package com.aicc.silverlink.domain.map.controller;

import com.aicc.silverlink.domain.map.dto.WelfareFacilityRequest;
import com.aicc.silverlink.domain.map.dto.WelfareFacilityResponse;
import com.aicc.silverlink.domain.map.service.WelfareFacilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/map/facilities")
@RequiredArgsConstructor
public class WelfareFacilityController {

    private final WelfareFacilityService welfareFacilityService;

    // --- 사용자/공통 API ---

    /**
     * 현재 위치 반경 내 사회복지시설 목록을 조회합니다
     *
     * @param latitude  사용자 현재 위도
     * @param longitude 사용자 현재 경도
     * @param radius    검색 반경 (km, 기본값 1km)
     * @return 사회복지시설 목록
     */
    @GetMapping("/nearby")
    public ResponseEntity<List<WelfareFacilityResponse>> getNearbyFacilities(
            @RequestParam("lat") Double latitude,
            @RequestParam("lon") Double longitude,
            @RequestParam(value = "radius", defaultValue = "1") Double radius) {
        List<WelfareFacilityResponse> facilities = welfareFacilityService.getFacilitiesWithinRadius(
                latitude, longitude, radius);
        return ResponseEntity.ok(facilities);
    }

    // 시설 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<WelfareFacilityResponse> getFacilityDetail(@PathVariable Long id) {
        return ResponseEntity.ok(welfareFacilityService.getFacilityDetail(id));
    }

    // --- 관리자(Admin) API ---
    // 실제 운영 시에는 @PreAuthorize("hasRole('ADMIN')") 등을 붙여 권한 제어 필요

    // 모든 시설 조회
    // 모든 시설 조회
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<WelfareFacilityResponse>> getAllFacilities() {
        return ResponseEntity.ok(welfareFacilityService.getAllFacilities());
    }

    // 시설 등록
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Long> createFacility(@RequestBody @Valid WelfareFacilityRequest request) { // @Valid 추가
        return ResponseEntity.ok(welfareFacilityService.createFacility(request));
    }

    // 시설 수정
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WelfareFacilityResponse> updateFacility(
            @PathVariable Long id,
            @RequestBody @Valid WelfareFacilityRequest request) { // @Valid 추가
        return ResponseEntity.ok(welfareFacilityService.updateFacility(id, request));
    }

    // 시설 삭제
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteFacility(@PathVariable Long id) {
        welfareFacilityService.deleteFacility(id);
        return ResponseEntity.ok().build();
    }
}
