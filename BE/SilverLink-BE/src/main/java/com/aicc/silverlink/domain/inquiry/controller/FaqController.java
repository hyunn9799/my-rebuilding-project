package com.aicc.silverlink.domain.inquiry.controller;

import com.aicc.silverlink.domain.inquiry.dto.FaqResponse;
import com.aicc.silverlink.domain.inquiry.service.FaqService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/faqs")
@RequiredArgsConstructor
@Tag(name = "FAQ", description = "FAQ API")
public class FaqController {

    private final FaqService faqService;

    @GetMapping
    @Operation(summary = "FAQ 목록 조회", description = "카테고리별 FAQ 목록을 조회하거나 키워드로 검색합니다. category, keyword 파라미터가 없으면 전체 목록을 반환합니다.")
    public ResponseEntity<List<FaqResponse>> getFaqs(@RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        List<FaqResponse> faqs = faqService.getFaqs(category, keyword);
        return ResponseEntity.ok(faqs);
    }
}
