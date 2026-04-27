package com.aicc.silverlink.domain.map.entity;

import com.aicc.silverlink.domain.map.dto.WelfareFacilityRequest;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "welfare_facilities")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class WelfareFacility {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "facility_id")
    private Long id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "address", nullable = false, length = 500)
    private String address;

    @Column(name = "latitude", nullable = false)
    private Double latitude; // 위도

    @Column(name = "longitude", nullable = false)
    private Double longitude; // 경도

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private WelfareFacilityType type; // 시설 종류 (Enum으로 정의)

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "operating_hours", length = 255)
    private String operatingHours;

    // TODO: 데이터베이스에 description 컬럼 수동 추가 필요
    // @Column(name = "description", columnDefinition = "TEXT")
    // private String description;

    // 기타 필요한 필드 추가 가능 (예: 웹사이트, 상세 설명 등)

    public Long getId() {
        return id;
    }

    public void update(WelfareFacilityRequest request) {
        this.name = request.getName();
        this.address = request.getAddress();
        this.latitude = request.getLatitude();
        this.longitude = request.getLongitude();
        this.type = request.getType();
        this.phone = request.getPhone();
        this.operatingHours = request.getOperatingHours();
        // TODO: description 필드 활성화 후 추가
        // this.description = request.getDescription();
    }
}
