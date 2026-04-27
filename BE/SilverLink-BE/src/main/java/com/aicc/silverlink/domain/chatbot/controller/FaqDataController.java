package com.aicc.silverlink.domain.chatbot.controller;

import com.aicc.silverlink.domain.chatbot.dto.FaqDataDto;
import com.aicc.silverlink.domain.chatbot.service.FaqDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Python 챗봇 서비스에 FAQ 데이터를 제공하는 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/data/faqs")
@RequiredArgsConstructor
@Tag(name = "Chatbot Data - FAQ", description = "챗봇 서비스용 FAQ 데이터 제공 API")
public class FaqDataController {

    private final FaqDataService faqDataService;

    @GetMapping("/all")
    @Operation(summary = "전체 FAQ 데이터 조회", description = "Python 챗봇 서비스의 초기 동기화 또는 전체 재동기화를 위해 모든 활성 FAQ 데이터를 반환합니다.")
    public ResponseEntity<List<FaqDataDto>> getAllActiveFaqs() {
        log.info("API called: GET /api/data/faqs/all");

        List<FaqDataDto> faqs = faqDataService.getAllActiveFaqs();

        log.info("Returning {} active FAQs", faqs.size());
        return ResponseEntity.ok(faqs);
    }

    @GetMapping("/updated-since")
    @Operation(summary = "특정 시간 이후 업데이트된 FAQ 조회", description = "Python 챗봇 서비스의 증분 동기화를 위해 특정 시간 이후 업데이트된 FAQ만 반환합니다.")
    public ResponseEntity<List<FaqDataDto>> getUpdatedFaqs(
            @Parameter(description = "이 시간 이후 업데이트된 FAQ만 조회 (ISO 8601 형식)", example = "2024-01-21T10:00:00") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since) {
        log.info("API called: GET /api/data/faqs/updated-since?since={}", since);

        List<FaqDataDto> faqs = faqDataService.getUpdatedFaqsSince(since);

        log.info("Returning {} updated FAQs since {}", faqs.size(), since);
        return ResponseEntity.ok(faqs);
    }
}
