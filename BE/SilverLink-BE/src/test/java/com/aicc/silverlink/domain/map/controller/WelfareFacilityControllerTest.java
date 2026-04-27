package com.aicc.silverlink.domain.map.controller;

import com.aicc.silverlink.domain.map.dto.WelfareFacilityRequest;
import com.aicc.silverlink.domain.map.dto.WelfareFacilityResponse;
import com.aicc.silverlink.domain.map.entity.WelfareFacilityType;
import com.aicc.silverlink.domain.map.service.WelfareFacilityService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class WelfareFacilityControllerTest {

    @InjectMocks
    private WelfareFacilityController welfareFacilityController;

    @Mock
    private WelfareFacilityService welfareFacilityService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        // setMessageConverters 제거하여 기본 컨버터 사용 (String, JSON 등 모두 지원)
        mockMvc = MockMvcBuilders.standaloneSetup(welfareFacilityController).build();
    }

    @Test
    @DisplayName("반경 내 사회복지시설 조회 API 테스트")
    void getNearbyFacilities() throws Exception {
        // given
        WelfareFacilityResponse response = WelfareFacilityResponse.builder()
                .id(1L)
                .name("서울노인복지센터")
                .address("서울시 종로구")
                .latitude(37.5700)
                .longitude(126.9800)
                .type(WelfareFacilityType.ELDERLY_WELFARE_CENTER)
                .build();

        given(welfareFacilityService.getFacilitiesWithinRadius(anyDouble(), anyDouble(), anyDouble()))
                .willReturn(List.of(response));

        // when & then
        mockMvc.perform(get("/api/map/facilities/nearby")
                        .param("lat", "37.5665")
                        .param("lon", "126.9780")
                        .param("radius", "1.0")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("서울노인복지센터"))
                .andExpect(jsonPath("$[0].type").value("ELDERLY_WELFARE_CENTER"));
    }

    @Test
    @DisplayName("사회복지시설 등록 API 테스트")
    void createFacility() throws Exception {
        // given
        WelfareFacilityRequest request = WelfareFacilityRequest.builder()
                .name("새로운 복지관")
                .address("서울시 어딘가")
                .latitude(37.1234)
                .longitude(127.1234)
                .type(WelfareFacilityType.COMMUNITY_WELFARE_CENTER)
                .build();

        given(welfareFacilityService.createFacility(any(WelfareFacilityRequest.class))).willReturn(1L);

        // when & then
        mockMvc.perform(post("/api/map/facilities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("1"));
    }

    @Test
    @DisplayName("사회복지시설 수정 API 테스트")
    void updateFacility() throws Exception {
        // given
        Long facilityId = 1L;
        WelfareFacilityRequest request = WelfareFacilityRequest.builder()
                .name("수정된 복지관")
                .address("서울시 다른곳")
                .latitude(37.5678)
                .longitude(127.5678)
                .type(WelfareFacilityType.ELDERLY_WELFARE_CENTER)
                .build();
        WelfareFacilityResponse response = WelfareFacilityResponse.builder()
                .id(facilityId)
                .name("수정된 복지관")
                .build();

        given(welfareFacilityService.updateFacility(eq(facilityId), any(WelfareFacilityRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(put("/api/map/facilities/{id}", facilityId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("수정된 복지관"));
    }

    @Test
    @DisplayName("사회복지시설 삭제 API 테스트")
    void deleteFacility() throws Exception {
        // given
        Long facilityId = 1L;

        // when & then
        mockMvc.perform(delete("/api/map/facilities/{id}", facilityId))
                .andDo(print())
                .andExpect(status().isOk());
    }
}
