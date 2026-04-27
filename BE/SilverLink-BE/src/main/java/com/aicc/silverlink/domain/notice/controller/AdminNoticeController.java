package com.aicc.silverlink.domain.notice.controller;

import com.aicc.silverlink.domain.admin.entity.Admin;
import com.aicc.silverlink.domain.admin.repository.AdminRepository;
import com.aicc.silverlink.domain.notice.dto.NoticeRequest;
import com.aicc.silverlink.domain.notice.dto.NoticeResponse;
import com.aicc.silverlink.domain.notice.service.NoticeService;
import com.aicc.silverlink.global.exception.UnauthorizedException;
import com.aicc.silverlink.global.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "공지사항 관리 (관리자)", description = "공지사항 등록/삭제 API (관리자 전용)")
@RestController
@RequestMapping("/api/admin/notices")
@RequiredArgsConstructor
public class AdminNoticeController {

    private final NoticeService noticeService;
    private final AdminRepository adminRepository;

    // 공지사항 목록 조회
    @GetMapping
    public ResponseEntity<Page<NoticeResponse>> getAllNotices(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(noticeService.getAllNoticesForAdmin(pageable));
    }

    // 공지사항 등록
    @PostMapping
    public ResponseEntity<Long> createNotice(@RequestBody @Valid NoticeRequest request) {
        
        // SecurityUtils를 통해 현재 사용자 ID 가져오기
        if (!SecurityUtils.isAuthenticated()) {
            throw new UnauthorizedException("인증이 필요합니다.");
        }
        
        Long userId = SecurityUtils.currentUserId();
        
        // 관리자 정보 조회
        Admin admin = adminRepository.findByUserId(userId)
                .orElseThrow(() -> new UnauthorizedException("관리자 권한이 필요합니다."));
        
        Long noticeId = noticeService.createNotice(request, admin);
        return ResponseEntity.ok(noticeId);
    }

    // 공지사항 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<NoticeResponse> getNotice(@PathVariable Long id) {
        return ResponseEntity.ok(noticeService.getNoticeDetail(id, null));
    }

    // Req 68: 공지사항 삭제 (Soft Delete)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotice(@PathVariable Long id) {
        
        if (!SecurityUtils.isAuthenticated()) {
            throw new UnauthorizedException("인증이 필요합니다.");
        }
        
        Long userId = SecurityUtils.currentUserId();
        Admin admin = adminRepository.findByUserId(userId)
                .orElseThrow(() -> new UnauthorizedException("관리자 권한이 필요합니다."));
        
        noticeService.deleteNotice(id, admin);
        return ResponseEntity.ok().build();
    }

    // 공지사항 수정
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateNotice(
            @PathVariable Long id,
            @RequestBody @Valid NoticeRequest request) {
        
        if (!SecurityUtils.isAuthenticated()) {
            throw new UnauthorizedException("인증이 필요합니다.");
        }
        
        Long userId = SecurityUtils.currentUserId();
        Admin admin = adminRepository.findByUserId(userId)
                .orElseThrow(() -> new UnauthorizedException("관리자 권한이 필요합니다."));
        
        noticeService.updateNotice(id, request, admin);
        return ResponseEntity.ok().build();
    }
}
