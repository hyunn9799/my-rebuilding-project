package com.aicc.silverlink.domain.elderly.dto.response;

import com.aicc.silverlink.domain.elderly.entity.ElderlyHealthInfo;

import java.time.LocalDateTime;

public record HealthInfoResponse(
        Long elderlyUserId,
        String chronicDiseases,
        String mentalHealthNotes,
        String specialNotes,
        LocalDateTime updatedAt
) {
    public static HealthInfoResponse from(ElderlyHealthInfo hi) {
        return new HealthInfoResponse(
                hi.getId(),
                hi.getChronicDiseases(),
                hi.getMentalHealthNotes(),
                hi.getSpecialNotes(),
                hi.getUpdatedAt()
        );
    }
}
