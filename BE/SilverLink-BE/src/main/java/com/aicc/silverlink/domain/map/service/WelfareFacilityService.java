package com.aicc.silverlink.domain.map.service;

import com.aicc.silverlink.domain.map.dto.WelfareFacilityRequest;
import com.aicc.silverlink.domain.map.dto.WelfareFacilityResponse;
import com.aicc.silverlink.domain.map.entity.WelfareFacility;
import com.aicc.silverlink.domain.map.repository.WelfareFacilityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WelfareFacilityService {

    private final WelfareFacilityRepository welfareFacilityRepository;

    // --- 사용자/공통 기능 ---

    // 반경 내 사회복지시설 조회
    public List<WelfareFacilityResponse> getFacilitiesWithinRadius(
            Double userLatitude, Double userLongitude, Double radiusKm) {
        
        // 좌표 유효성 검사
        if (userLatitude < -90 || userLatitude > 90 || userLongitude < -180 || userLongitude > 180) {
            throw new IllegalArgumentException("유효하지 않은 좌표입니다.");
        }

        List<WelfareFacility> facilities = welfareFacilityRepository.findFacilitiesWithinRadius(
                userLatitude, userLongitude, radiusKm);
        return facilities.stream()
                .map(WelfareFacilityResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // 모든 사회복지시설 조회 (관리자용)
    public List<WelfareFacilityResponse> getAllFacilities() {
        List<WelfareFacility> facilities = welfareFacilityRepository.findAll();
        return facilities.stream()
                .map(WelfareFacilityResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // 특정 시설 상세 조회
    public WelfareFacilityResponse getFacilityDetail(Long facilityId) {
        WelfareFacility facility = welfareFacilityRepository.findById(facilityId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 시설입니다."));
        return WelfareFacilityResponse.fromEntity(facility);
    }

    // --- 관리자(Admin) 기능 ---

    // 사회복지시설 등록
    @Transactional
    public Long createFacility(WelfareFacilityRequest request) {
        WelfareFacility facility = WelfareFacility.builder()
                .name(request.getName())
                .address(request.getAddress())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .type(request.getType())
                .phone(request.getPhone())
                .operatingHours(request.getOperatingHours())
                // TODO: description 필드 활성화 후 추가
                // .description(request.getDescription())
                .build();
        WelfareFacility savedFacility = welfareFacilityRepository.save(facility);
        return savedFacility.getId();
    }

    // 사회복지시설 수정
    @Transactional
    public WelfareFacilityResponse updateFacility(Long facilityId, WelfareFacilityRequest request) {
        WelfareFacility facility = welfareFacilityRepository.findById(facilityId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 시설입니다."));
        facility.update(request); // 엔티티의 update 메서드 호출
        WelfareFacility updatedFacility = welfareFacilityRepository.save(facility); // 변경 감지 후 저장
        return WelfareFacilityResponse.fromEntity(updatedFacility);
    }

    // 사회복지시설 삭제
    @Transactional
    public void deleteFacility(Long facilityId) {
        if (!welfareFacilityRepository.existsById(facilityId)) {
            throw new IllegalArgumentException("존재하지 않는 시설입니다.");
        }
        welfareFacilityRepository.deleteById(facilityId);
    }
}
