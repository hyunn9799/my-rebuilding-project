package com.aicc.silverlink.domain.inquiry.controller;

import com.aicc.silverlink.domain.inquiry.dto.InquiryRequest;
import com.aicc.silverlink.domain.inquiry.dto.InquiryResponse;
import com.aicc.silverlink.domain.inquiry.service.InquiryService;
import com.aicc.silverlink.domain.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inquiries")
@RequiredArgsConstructor
@Tag(name = "1:1 문의", description = "1:1 문의 관리 API")
public class InquiryController {

    private final InquiryService inquiryService;
    private final com.aicc.silverlink.domain.user.repository.UserRepository userRepository;

    @GetMapping
    @Operation(summary = "문의 목록 조회", description = "보호자는 매핑된 어르신의 문의, 상담사는 담당 어르신의 문의 목록을 조회합니다.")
    public ResponseEntity<List<InquiryResponse>> getInquiries(@AuthenticationPrincipal Long userId) {
        User user = getUser(userId);
        return ResponseEntity.ok(inquiryService.getInquiries(user));
    }

    @GetMapping("/{id}")
    @Operation(summary = "문의 상세 조회", description = "특정 문의의 상세 내용을 조회합니다.")
    public ResponseEntity<InquiryResponse> getInquiry(@PathVariable Long id, @AuthenticationPrincipal Long userId) {
        User user = getUser(userId);
        return ResponseEntity.ok(inquiryService.getInquiry(id, user));
    }

    @PostMapping
    @Operation(summary = "문의 등록", description = "보호자가 1:1 문의를 등록합니다.")
    public ResponseEntity<InquiryResponse> createInquiry(@RequestBody InquiryRequest request,
            @AuthenticationPrincipal Long userId) {
        User user = getUser(userId);
        return ResponseEntity.ok(inquiryService.createInquiry(user, request));
    }

    @PostMapping("/{id}/answer")
    @Operation(summary = "답변 등록", description = "상담사가 문의에 대한 답변을 등록합니다.")
    public ResponseEntity<Void> registerAnswer(@PathVariable Long id, @RequestBody InquiryRequest request,
            @AuthenticationPrincipal Long userId) {
        User user = getUser(userId);
        inquiryService.registerAnswer(id, user, request);
        return ResponseEntity.ok().build();
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }
}
