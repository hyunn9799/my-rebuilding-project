package com.aicc.silverlink.domain.notice.controller;

import com.aicc.silverlink.domain.notice.dto.NoticeResponse;
import com.aicc.silverlink.domain.notice.service.NoticeService;
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "공지사항", description = "공지사항 조회 API")
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    // Req 64, 65: 내 권한에 맞는 공지사항 목록 조회 (중요공지 상단)
    @GetMapping
    public ResponseEntity<Page<NoticeResponse>> getMyNotices(
            @RequestParam(required = false) String keyword, // 검색 키워드 추가
            Pageable pageable,
            Authentication authentication) { // Authentication으로 변경
        
        System.out.println("=== NoticeController.getMyNotices 호출 ===");
        
        User user = null;
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            System.out.println("Principal 타입: " + (principal != null ? principal.getClass().getName() : "null"));
            System.out.println("Principal 값: " + principal);
            
            if (principal instanceof Long) {
                Long userId = (Long) principal;
                System.out.println("사용자 ID: " + userId);
                // User 조회 로직 필요 - UserRepository 주입 필요
                // 임시로 역할만 가져오기
                String roleStr = authentication.getAuthorities().stream()
                        .findFirst()
                        .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                        .orElse(null);
                System.out.println("사용자 역할 (권한에서): " + roleStr);
                
                // User 객체 생성 (최소한의 정보만)
                if (roleStr != null) {
                    user = User.createFake(userId, Role.valueOf(roleStr));
                }
            } else if (principal instanceof User) {
                user = (User) principal;
                System.out.println("사용자 정보: " + user.getId() + "(" + user.getRole() + ")");
            }
        }
        
        System.out.println("최종 사용자 정보: " + (user != null ? user.getId() + "(" + user.getRole() + ")" : "null"));
        System.out.println("키워드: " + keyword);
        
        Page<NoticeResponse> result = noticeService.getNoticesForUser(user, keyword, pageable);
        
        System.out.println("반환할 공지사항 수: " + result.getContent().size());
        for (NoticeResponse notice : result.getContent()) {
            System.out.println("  - ID: " + notice.getId() + ", 제목: " + notice.getTitle() + 
                             ", 대상모드: " + notice.getTargetMode() + 
                             ", 대상역할: " + notice.getTargetRoles());
        }
        
        return ResponseEntity.ok(result);
    }

    // Req 67: 메인 화면 팝업 공지 조회
    @GetMapping("/popups")
    public ResponseEntity<List<NoticeResponse>> getPopups(Authentication authentication) {
        User user = extractUserFromAuthentication(authentication);
        return ResponseEntity.ok(noticeService.getActivePopupsForUser(user));
    }

    // 공지사항 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<NoticeResponse> getNoticeDetail(
            @PathVariable Long id,
            Authentication authentication) {
        User user = extractUserFromAuthentication(authentication);
        return ResponseEntity.ok(noticeService.getNoticeDetail(id, user));
    }

    // Req 69: 공지사항 필독 확인 ("확인했습니다" 버튼 클릭 시)
    @PostMapping("/{id}/read")
    public ResponseEntity<Void> readNotice(
            @PathVariable Long id,
            Authentication authentication) {
        User user = extractUserFromAuthentication(authentication);
        if (user != null) {
            noticeService.readNotice(id, user);
        }
        return ResponseEntity.ok().build();
    }

    // 프론트엔드 호환용 - /confirm 별칭 (동일 동작)
    @PostMapping("/{id}/confirm")
    public ResponseEntity<Void> confirmNotice(
            @PathVariable Long id,
            Authentication authentication) {
        User user = extractUserFromAuthentication(authentication);
        if (user != null) {
            noticeService.readNotice(id, user);
        }
        return ResponseEntity.ok().build();
    }
    
    // Helper: Authentication에서 User 추출
    private User extractUserFromAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long) {
            Long userId = (Long) principal;
            String roleStr = authentication.getAuthorities().stream()
                    .findFirst()
                    .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                    .orElse(null);
            
            if (roleStr != null) {
                return User.createFake(userId, Role.valueOf(roleStr));
            }
        } else if (principal instanceof User) {
            return (User) principal;
        }
        
        return null;
    }
}
