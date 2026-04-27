package com.aicc.silverlink.domain.guardian.dto;

import com.aicc.silverlink.domain.guardian.entity.GuardianElderly;
import com.aicc.silverlink.domain.guardian.entity.RelationType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class GuardianElderlyResponse {

    private Long id; // 관계 고유 ID

    // 1. 보호자 정보 (엔티티 대신 필요한 값만!)
    private Long guardianId;
    private String guardianName;
    private String guardianPhone;

    // 2. 어르신 정보 (엔티티 대신 필요한 값만!)
    private Long elderlyId;
    private String elderlyName;
    private String elderlyPhone;

    // 3. 관계 정보
    private RelationType relationType;
    private LocalDateTime connectedAt; // 등록일

    public static GuardianElderlyResponse from(GuardianElderly entity) {
        // 엔티티에서 깊숙이 들어가서 데이터를 꺼냅니다.
        // (Repository에서 Fetch Join을 해뒀기 때문에 성능 문제 없음)

        return GuardianElderlyResponse.builder()
                .id(entity.getId())

                // 보호자 쪽 데이터 꺼내기
                .guardianId(entity.getGuardian().getId())
                .guardianName(entity.getGuardian().getUser().getName())
                .guardianPhone(entity.getGuardian().getUser().getPhone())

                // 어르신 쪽 데이터 꺼내기
                .elderlyId(entity.getElderly().getId())
                .elderlyName(entity.getElderly().getUser().getName())
                .elderlyPhone(entity.getElderly().getUser().getPhone())

                // 관계 정보 꺼내기
                .relationType(entity.getRelationType())
                .connectedAt(entity.getCreatedAt())
                .build();
    }
}