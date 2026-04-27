package com.aicc.silverlink.domain.chatbot.service;

import com.aicc.silverlink.domain.chatbot.dto.InquiryDataDto;
import com.aicc.silverlink.domain.inquiry.entity.Inquiry;
import com.aicc.silverlink.domain.inquiry.entity.Inquiry.InquiryStatus;
import com.aicc.silverlink.domain.inquiry.repository.InquiryAnswerRepository;
import com.aicc.silverlink.domain.inquiry.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Python 챗봇 서비스에 Inquiry 데이터를 제공하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryDataService {

    private final InquiryRepository inquiryRepository;
    private final InquiryAnswerRepository inquiryAnswerRepository;

    /**
     * 답변 완료된 모든 문의 데이터 조회
     * Python 챗봇의 초기 동기화 또는 전체 재동기화 시 사용
     *
     * @return 답변 완료된 모든 문의 목록
     */
    public List<InquiryDataDto> getAllAnsweredInquiries() {
        log.info("Retrieving all answered inquiries for chatbot sync");

        List<Inquiry> inquiries = inquiryRepository
                .findAllByStatusAndIsDeletedFalseOrderByCreatedAtDesc(InquiryStatus.ANSWERED);

        return inquiries.stream()
                .map(inquiry -> {
                    var answer = inquiryAnswerRepository
                            .findByInquiryIdAndIsDeletedFalse(inquiry.getId())
                            .orElse(null);
                    return InquiryDataDto.from(inquiry, answer);
                })
                .collect(Collectors.toList());
    }

    /**
     * 특정 보호자-어르신 관계의 답변 완료된 문의 조회
     * Python 챗봇의 권한 필터링 및 개인화된 답변 생성 시 사용
     *
     * @param guardianId 보호자 ID
     * @param elderlyId  어르신 ID
     * @return 해당 관계의 답변 완료된 문의 목록
     */
    public List<InquiryDataDto> getInquiriesByRelation(Long guardianId, Long elderlyId) {
        log.info("Retrieving answered inquiries for guardian: {} and elderly: {}",
                guardianId, elderlyId);

        List<Inquiry> inquiries = inquiryRepository
                .findAnsweredInquiriesByRelation(guardianId, elderlyId);

        return inquiries.stream()
                .map(inquiry -> {
                    var answer = inquiryAnswerRepository
                            .findByInquiryIdAndIsDeletedFalse(inquiry.getId())
                            .orElse(null);
                    return InquiryDataDto.from(inquiry, answer);
                })
                .collect(Collectors.toList());
    }
}
