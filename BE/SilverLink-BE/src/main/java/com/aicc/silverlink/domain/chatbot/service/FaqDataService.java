package com.aicc.silverlink.domain.chatbot.service;

import com.aicc.silverlink.domain.chatbot.dto.FaqDataDto;
import com.aicc.silverlink.domain.inquiry.repository.FaqRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Python 챗봇 서비스에 FAQ 데이터를 제공하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FaqDataService {

    private final FaqRepository faqRepository;

    /**
     * 모든 활성 FAQ 데이터 조회
     * Python 챗봇의 초기 동기화 또는 전체 재동기화 시 사용
     *
     * @return 모든 활성 FAQ 목록
     */
    public List<FaqDataDto> getAllActiveFaqs() {
        log.info("Retrieving all active FAQs for chatbot sync");

        return faqRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(FaqDataDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 시간 이후 업데이트된 FAQ 데이터 조회
     * Python 챗봇의 증분 동기화 시 사용
     *
     * @param since 이 시간 이후 업데이트된 FAQ만 조회
     * @return 업데이트된 FAQ 목록
     */
    public List<FaqDataDto> getUpdatedFaqsSince(LocalDateTime since) {
        log.info("Retrieving FAQs updated since: {}", since);

        return faqRepository.findAllByIsActiveTrueAndUpdatedAtAfterOrderByUpdatedAtAsc(since)
                .stream()
                .map(FaqDataDto::from)
                .collect(Collectors.toList());
    }
}
