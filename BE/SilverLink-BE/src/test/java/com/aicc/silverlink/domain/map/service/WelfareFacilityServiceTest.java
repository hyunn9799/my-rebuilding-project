package com.aicc.silverlink.domain.map.service;

import com.aicc.silverlink.domain.map.dto.WelfareFacilityRequest;
import com.aicc.silverlink.domain.map.dto.WelfareFacilityResponse;
import com.aicc.silverlink.domain.map.entity.WelfareFacility;
import com.aicc.silverlink.domain.map.entity.WelfareFacilityType;
import com.aicc.silverlink.domain.map.repository.WelfareFacilityRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WelfareFacilityServiceTest {

    @InjectMocks
    private WelfareFacilityService welfareFacilityService;

    @Mock
    private WelfareFacilityRepository welfareFacilityRepository;

    @Test
    @DisplayName("반경 내 사회복지시설 조회 테스트")
    void getFacilitiesWithinRadius() {
        // given
        Double userLat = 37.5665;
        Double userLon = 126.9780;
        Double radius = 1.0;

        WelfareFacility facility = WelfareFacility.builder()
                .id(1L)
                .name("서울노인복지센터")
                .address("서울시 종로구")
                .latitude(37.5700)
                .longitude(126.9800)
                .type(WelfareFacilityType.ELDERLY_WELFARE_CENTER)
                .build();

        given(welfareFacilityRepository.findFacilitiesWithinRadius(userLat, userLon, radius))
                .willReturn(List.of(facility));

        // when
        List<WelfareFacilityResponse> responses = welfareFacilityService.getFacilitiesWithinRadius(userLat, userLon, radius);

        // then
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("서울노인복지센터", responses.get(0).getName());
        verify(welfareFacilityRepository).findFacilitiesWithinRadius(userLat, userLon, radius);
    }

    @Test
    @DisplayName("반경 내 사회복지시설 조회 실패 테스트 - 잘못된 좌표")
    void getFacilitiesWithinRadius_InvalidCoordinates() {
        // given
        Double invalidLat = 100.0; // 위도 범위 초과
        Double userLon = 126.9780;
        Double radius = 1.0;

        // when & then
        assertThrows(IllegalArgumentException.class, () ->
                welfareFacilityService.getFacilitiesWithinRadius(invalidLat, userLon, radius));
    }

    @Test
    @DisplayName("사회복지시설 등록 테스트")
    void createFacility() {
        // given
        WelfareFacilityRequest request = WelfareFacilityRequest.builder()
                .name("새로운 복지관")
                .address("서울시 어딘가")
                .latitude(37.1234)
                .longitude(127.1234)
                .type(WelfareFacilityType.COMMUNITY_WELFARE_CENTER)
                .build();

        WelfareFacility savedFacility = WelfareFacility.builder()
                .id(1L)
                .name("새로운 복지관")
                .build();

        given(welfareFacilityRepository.save(any(WelfareFacility.class))).willReturn(savedFacility);

        // when
        Long facilityId = welfareFacilityService.createFacility(request);

        // then
        assertNotNull(facilityId);
        assertEquals(1L, facilityId);
        verify(welfareFacilityRepository).save(any(WelfareFacility.class));
    }

    @Test
    @DisplayName("사회복지시설 수정 테스트")
    void updateFacility() {
        // given
        Long facilityId = 1L;
        WelfareFacilityRequest request = WelfareFacilityRequest.builder()
                .name("수정된 복지관")
                .address("서울시 다른곳")
                .latitude(37.5678)
                .longitude(127.5678)
                .type(WelfareFacilityType.ELDERLY_WELFARE_CENTER)
                .build();

        WelfareFacility existingFacility = WelfareFacility.builder()
                .id(facilityId)
                .name("원래 복지관")
                .build();

        given(welfareFacilityRepository.findById(facilityId)).willReturn(Optional.of(existingFacility));
        given(welfareFacilityRepository.save(any(WelfareFacility.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        WelfareFacilityResponse response = welfareFacilityService.updateFacility(facilityId, request);

        // then
        assertNotNull(response);
        assertEquals("수정된 복지관", response.getName());
        verify(welfareFacilityRepository).findById(facilityId);
        verify(welfareFacilityRepository).save(any(WelfareFacility.class));
    }

    @Test
    @DisplayName("사회복지시설 삭제 테스트")
    void deleteFacility() {
        // given
        Long facilityId = 1L;
        given(welfareFacilityRepository.existsById(facilityId)).willReturn(true);

        // when
        welfareFacilityService.deleteFacility(facilityId);

        // then
        verify(welfareFacilityRepository).existsById(facilityId);
        verify(welfareFacilityRepository).deleteById(facilityId);
    }
}
