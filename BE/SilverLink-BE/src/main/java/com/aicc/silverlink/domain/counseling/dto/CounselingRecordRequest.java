package com.aicc.silverlink.domain.counseling.dto;

import com.aicc.silverlink.domain.counseling.entity.CounselingRecord.CounselingStatus;
import com.aicc.silverlink.domain.counseling.entity.CounselingRecord.CounselingType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@NoArgsConstructor
public class CounselingRecordRequest {
    @NotNull
    private Long seniorId;

    @NotNull
    private LocalDate date;

    @NotNull
    private LocalTime time;

    @NotNull
    private CounselingType type;

    private String category;
    private String summary;
    private String content;
    private String result;
    private String followUp;

    @NotNull
    private CounselingStatus status;
}
