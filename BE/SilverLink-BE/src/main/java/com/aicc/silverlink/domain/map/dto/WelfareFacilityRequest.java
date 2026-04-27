package com.aicc.silverlink.domain.map.dto;

import com.aicc.silverlink.domain.map.entity.WelfareFacilityType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class WelfareFacilityRequest {

    @NotBlank(message = "시설명은 필수입니다.")
    private String name;

    @NotBlank(message = "주소는 필수입니다.")
    private String address;

    @NotNull(message = "위도는 필수입니다.")
    @Min(value = -90, message = "위도는 -90 이상이어야 합니다.")
    @Max(value = 90, message = "위도는 90 이하이어야 합니다.")
    private Double latitude;

    @NotNull(message = "경도는 필수입니다.")
    @Min(value = -180, message = "경도는 -180 이상이어야 합니다.")
    @Max(value = 180, message = "경도는 180 이하이어야 합니다.")
    private Double longitude;

    @NotNull(message = "시설 종류는 필수입니다.")
    private WelfareFacilityType type;

    private String phone;
    private String operatingHours;
    // TODO: 백엔드 description 필드 활성화 후 사용
    // private String description;
}
