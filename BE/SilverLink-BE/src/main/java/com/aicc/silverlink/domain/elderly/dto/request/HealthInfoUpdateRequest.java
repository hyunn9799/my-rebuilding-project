package com.aicc.silverlink.domain.elderly.dto.request;

import jakarta.validation.constraints.Size;

public record HealthInfoUpdateRequest(
        @Size(max = 20000) String chronicDiseases,
        @Size(max = 20000) String mentalHealthNotes,
        @Size(max = 20000) String specialNotes
) {}
