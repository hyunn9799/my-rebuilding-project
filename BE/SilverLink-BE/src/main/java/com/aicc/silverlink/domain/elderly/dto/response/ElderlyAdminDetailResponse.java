package com.aicc.silverlink.domain.elderly.dto.response;

import com.aicc.silverlink.domain.counselor.dto.CounselorResponse;
import com.aicc.silverlink.domain.guardian.dto.GuardianResponse;
import lombok.Builder;

@Builder
public record ElderlyAdminDetailResponse(
        ElderlySummaryResponse elderly,
        GuardianResponse guardian,
        CounselorResponse counselor
) {}