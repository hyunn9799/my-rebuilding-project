package com.aicc.silverlink.domain.map.dto;

import com.aicc.silverlink.domain.map.entity.WelfareFacility;
import com.aicc.silverlink.domain.map.entity.WelfareFacilityType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class WelfareFacilityResponse {
    private Long id;
    private String name;
    private String address;
    private Double latitude;
    private Double longitude;
    private WelfareFacilityType type;
    private String phone;
    private String operatingHours;
    // TODO: 백엔드 description 필드 활성화 후 사용
    // private String description;
    private String typeDescription; // Enum의 설명을 위한 필드

    public static WelfareFacilityResponse fromEntity(WelfareFacility facility) {
        return WelfareFacilityResponse.builder()
                .id(facility.getId())
                .name(facility.getName())
                .address(facility.getAddress())
                .latitude(facility.getLatitude())
                .longitude(facility.getLongitude())
                .type(facility.getType())
                .phone(facility.getPhone())
                .operatingHours(facility.getOperatingHours())
                // TODO: description 필드 활성화 후 추가
                // .description(facility.getDescription())
                .typeDescription(facility.getType() != null ? facility.getType().getDescription() : null)
                .build();
    }
}
