package com.aicc.silverlink.domain.chatbot.controller;

import com.aicc.silverlink.domain.chatbot.dto.InquiryDataDto;
import com.aicc.silverlink.domain.chatbot.service.InquiryDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Python 챗봇 서비스에 Inquiry 데이터를 제공하는 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/data/inquiries")
@RequiredArgsConstructor
@Tag(name = "Chatbot Data - Inquiry", description = "챗봇 서비스용 문의 데이터 제공 API")
public class InquiryDataController {

    private final InquiryDataService inquiryDataService;

    @GetMapping("/answered")
    @Operation(summary = "답변 완료된 전체 문의 조회", description = "Python 챗봇 서비스의 초기 동기화 또는 전체 재동기화를 위해 답변 완료된 모든 문의 데이터를 반환합니다.")
    public ResponseEntity<List<InquiryDataDto>> getAllAnsweredInquiries() {
        log.info("API called: GET /api/data/inquiries/answered");

        List<InquiryDataDto> inquiries = inquiryDataService.getAllAnsweredInquiries();

        log.info("Returning {} answered inquiries", inquiries.size());
        return ResponseEntity.ok(inquiries);
    }

    @GetMapping("/by-guardian/{guardianId}/elderly/{elderlyId}")
    @Operation(summary = "특정 보호자-어르신 관계의 답변 완료된 문의 조회", description = "Python 챗봇 서비스의 권한 필터링 및 개인화된 답변 생성을 위해 특정 보호자-어르신 관계의 문의만 반환합니다.")
    public ResponseEntity<List<InquiryDataDto>> getInquiriesByRelation(
            @Parameter(description = "보호자 사용자 ID") @PathVariable Long guardianId,
            @Parameter(description = "어르신 사용자 ID") @PathVariable Long elderlyId) {
        log.info("API called: GET /api/data/inquiries/by-guardian/{}/elderly/{}",
                guardianId, elderlyId);

        List<InquiryDataDto> inquiries = inquiryDataService
                .getInquiriesByRelation(guardianId, elderlyId);

        log.info("Returning {} inquiries for guardian {} and elderly {}",
                inquiries.size(), guardianId, elderlyId);
        return ResponseEntity.ok(inquiries);
    }
}
