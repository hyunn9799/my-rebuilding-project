package com.aicc.silverlink.domain.counseling.dto;

import com.aicc.silverlink.domain.counseling.entity.CounselingRecord;
import com.aicc.silverlink.domain.counseling.entity.CounselingRecord.CounselingStatus;
import com.aicc.silverlink.domain.counseling.entity.CounselingRecord.CounselingType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
public class CounselingRecordResponse {
    private Long id;
    private Long seniorId;
    private String seniorName;
    private LocalDate date;
    private LocalTime time;
    private CounselingType type;
    private String category;
    private String summary;
    private String content;
    private String result;
    private String followUp;
    private CounselingStatus status;

    public static CounselingRecordResponse from(CounselingRecord record) {
        return CounselingRecordResponse.builder()
                .id(record.getId())
                .seniorId(record.getElderly().getId())
                .seniorName(record.getElderly().getUser().getName())
                .date(record.getCounselingDate())
                .time(record.getCounselingTime())
                .type(record.getType())
                .category(record.getCategory())
                .summary(record.getSummary())
                .content(record.getContent())
                .result(record.getResult())
                .followUp(record.getFollowUp())
                .status(record.getStatus())
                .build();
    }
}
