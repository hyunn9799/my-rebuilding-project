package com.aicc.silverlink.domain.welfare.controller;

import com.aicc.silverlink.domain.welfare.dto.WelfareDetailResponse;
import com.aicc.silverlink.domain.welfare.dto.WelfareListResponse;
import com.aicc.silverlink.domain.welfare.dto.WelfareSearchRequest;
import com.aicc.silverlink.domain.welfare.service.WelfareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/welfare")
@Tag(name = "복지 서비스 관리 API", description = "공공데이터 수집 및 조회 관련 API")
public class WelfareController {

    private final WelfareService welfareService;

    @PostMapping("/sync/manual")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "데이터 수동 동기화 (관리자)", description = "공공데이터 포털에서 노인 복지 데이터를 즉시 수집합니다.")
    public ResponseEntity<String> manualSync() {
        welfareService.syncAllWelfareScheduled();
        return ResponseEntity.ok("데이터 동기화 작업이 시작되었습니다. (로그 확인 요망)");
    }

    @GetMapping
    @Operation(summary = "복지 서비스 검색 및 목록 조회", description = "검색어, 지역, 카테고리 등으로 복지 서비스를 검색합니다. (페이징 포함)")
    public ResponseEntity<Page<WelfareListResponse>> searchWelfare(
            @ModelAttribute WelfareSearchRequest request, // 검색 조건 (DTO)
            Pageable pageable // 페이징 정보 (page=0&size=10 등)
    ) {
        Page<WelfareListResponse> result = welfareService.searchWelfare(request, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{welfareId}")
    @Operation(summary = "복지 서비스 상세 조회", description = "서비스 ID로 상세 정보를 조회합니다.")
    public ResponseEntity<WelfareDetailResponse> getWelfareDetail(@PathVariable Long welfareId) {
        WelfareDetailResponse result = welfareService.getWelfareDetail(welfareId);
        return ResponseEntity.ok(result);
    }

}
