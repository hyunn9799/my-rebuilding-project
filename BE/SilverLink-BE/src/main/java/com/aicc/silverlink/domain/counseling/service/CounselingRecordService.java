package com.aicc.silverlink.domain.counseling.service;

import com.aicc.silverlink.domain.counseling.dto.CounselingRecordRequest;
import com.aicc.silverlink.domain.counseling.dto.CounselingRecordResponse;
import com.aicc.silverlink.domain.counseling.entity.CounselingRecord;
import com.aicc.silverlink.domain.counseling.repository.CounselingRecordRepository;
import com.aicc.silverlink.domain.counselor.entity.Counselor;
import com.aicc.silverlink.domain.counselor.repository.CounselorRepository;
import com.aicc.silverlink.domain.elderly.entity.Elderly;
import com.aicc.silverlink.domain.elderly.repository.ElderlyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CounselingRecordService {

    private final CounselingRecordRepository counselingRecordRepository;
    private final CounselorRepository counselorRepository;
    private final ElderlyRepository elderlyRepository;

    @Transactional
    public CounselingRecordResponse createRecord(Long counselorId, CounselingRecordRequest request) {
        Counselor counselor = counselorRepository.findById(counselorId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid counselor ID"));

        Elderly elderly = elderlyRepository.findById(request.getSeniorId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid elderly ID"));

        CounselingRecord record = CounselingRecord.builder()
                .counselor(counselor)
                .elderly(elderly)
                .counselingDate(request.getDate())
                .counselingTime(request.getTime())
                .type(request.getType())
                .category(request.getCategory())
                .summary(request.getSummary())
                .content(request.getContent())
                .result(request.getResult())
                .followUp(request.getFollowUp())
                .status(request.getStatus())
                .build();

        CounselingRecord savedRecord = counselingRecordRepository.save(record);
        return CounselingRecordResponse.from(savedRecord);
    }

    public List<CounselingRecordResponse> getMyRecords(Long counselorId) {
        return counselingRecordRepository.findByCounselorIdOrderByCounselingDateDescCounselingTimeDesc(counselorId)
                .stream()
                .map(CounselingRecordResponse::from)
                .collect(Collectors.toList());
    }

    public List<CounselingRecordResponse> getRecordsByElderly(Long elderlyId) {
        return counselingRecordRepository.findByElderlyIdOrderByCounselingDateDescCounselingTimeDesc(elderlyId).stream()
                .map(CounselingRecordResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public CounselingRecordResponse updateRecord(Long counselorId, Long recordId, CounselingRecordRequest request) {
        CounselingRecord record = counselingRecordRepository.findById(recordId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid record ID"));

        if (!record.getCounselor().getId().equals(counselorId)) {
            throw new IllegalArgumentException("Unauthorized to update this record");
        }

        record.update(
                request.getDate(),
                request.getTime(),
                request.getType(),
                request.getCategory(),
                request.getSummary(),
                request.getContent(),
                request.getResult(),
                request.getFollowUp(),
                request.getStatus());

        return CounselingRecordResponse.from(record);
    }
}
