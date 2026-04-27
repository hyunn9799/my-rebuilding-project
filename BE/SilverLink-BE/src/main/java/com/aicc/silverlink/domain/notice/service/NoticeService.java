package com.aicc.silverlink.domain.notice.service;

import com.aicc.silverlink.domain.admin.entity.Admin;
import com.aicc.silverlink.domain.notice.dto.NoticeRequest;
import com.aicc.silverlink.domain.notice.dto.NoticeResponse;
import com.aicc.silverlink.domain.notice.entity.*;
import com.aicc.silverlink.domain.notice.entity.Notice.NoticeStatus;
import com.aicc.silverlink.domain.notice.repository.NoticeReadLogRepository;
import com.aicc.silverlink.domain.notice.repository.NoticeRepository;
import com.aicc.silverlink.domain.notice.repository.NoticeTargetRoleRepository;
import com.aicc.silverlink.domain.notice.repository.NoticeAttachmentRepository;
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final NoticeReadLogRepository noticeReadLogRepository;
    private final NoticeTargetRoleRepository noticeTargetRoleRepository;
    private final NoticeAttachmentRepository noticeAttachmentRepository;

    // --- 관리자(Admin) 기능 ---

    // 공지사항 생성
    @Transactional
    public Long createNotice(NoticeRequest request, Admin admin) {
        // Admin이 null이면 예외 발생 (데이터베이스 NOT NULL 제약조건에 맞춤)
        if (admin == null) {
            throw new IllegalArgumentException("공지사항 작성자(관리자) 정보가 필요합니다.");
        }
        
        System.out.println("=== 공지사항 생성 시작 ===");
        System.out.println("제목: " + request.getTitle());
        System.out.println("대상 모드: " + request.getTargetMode());
        System.out.println("대상 역할들: " + request.getTargetRoles());
        
        // 1. 공지사항 본문 저장
        Notice notice = Notice.builder()
                .createdBy(admin) // admin은 반드시 존재해야 함
                .title(request.getTitle())
                .content(request.getContent())
                .category(request.getCategory())
                .targetMode(request.getTargetMode())
                .isPriority(request.isPriority())
                .isPopup(request.isPopup())
                .status(request.getStatus() != null ? request.getStatus() : NoticeStatus.PUBLISHED)
                .popupStartAt(request.getPopupStartAt())
                .popupEndAt(request.getPopupEndAt())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Notice savedNotice = noticeRepository.save(notice);
        System.out.println("공지사항 저장 완료, ID: " + savedNotice.getId());

        // 2. 타겟 권한 저장 (여러 역할 지원)
        if (request.getTargetMode() == Notice.TargetMode.ROLE_SET && request.getTargetRoles() != null && !request.getTargetRoles().isEmpty()) {
            System.out.println("대상 역할 저장 시작... (총 " + request.getTargetRoles().size() + "개)");
            List<NoticeTargetRole> targetRoles = request.getTargetRoles().stream()
                    .map(role -> {
                        System.out.println("  - 역할 저장: " + role);
                        NoticeTargetRole ntr = NoticeTargetRole.builder()
                                .notice(savedNotice)
                                .targetRole(role)
                                .build();
                        System.out.println("  - NoticeTargetRole 생성: noticeId=" + savedNotice.getId() + ", role=" + role);
                        return ntr;
                    })
                    .collect(Collectors.toList());
            
            List<NoticeTargetRole> savedRoles = noticeTargetRoleRepository.saveAll(targetRoles);
            System.out.println("대상 역할 저장 완료, 저장된 개수: " + savedRoles.size());
            
            // 저장 확인
            List<NoticeTargetRole> verifyRoles = noticeTargetRoleRepository.findAllByNoticeId(savedNotice.getId());
            System.out.println("저장 확인: DB에서 조회된 역할 수 = " + verifyRoles.size());
            verifyRoles.forEach(vr -> System.out.println("  - 확인된 역할: " + vr.getTargetRole()));
        } else if (request.getTargetMode() == Notice.TargetMode.ROLE_SET) {
            System.out.println("경고: ROLE_SET 모드이지만 대상 역할이 비어있습니다.");
        } else {
            System.out.println("대상 모드가 ALL이므로 역할 저장을 건너뜁니다.");
        }

        // 3. 첨부파일 저장 (Req 66)
        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            System.out.println("첨부파일 저장 시작... (총 " + request.getAttachments().size() + "개)");
            List<NoticeAttachment> attachments = request.getAttachments().stream()
                    .map(dto -> NoticeAttachment.builder()
                            .notice(savedNotice)
                            .fileName(dto.getFileName())
                            .originalFileName(dto.getOriginalFileName())
                            .filePath(dto.getFilePath())
                            .fileSize(dto.getFileSize())
                            .build())
                    .collect(Collectors.toList());
            noticeAttachmentRepository.saveAll(attachments);
            System.out.println("첨부파일 저장 완료");
        }

        System.out.println("=== 공지사항 생성 완료 ===");
        return savedNotice.getId();
    }

    // Req 68: 삭제 정책 (Soft Delete)
    @Transactional
    public void deleteNotice(Long noticeId, Admin admin) {
        if (admin == null) {
            throw new IllegalArgumentException("공지사항 삭제 권한이 없습니다.");
        }
        
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공지사항입니다."));

        // Soft Delete - 엔티티의 markAsDeleted 메서드 사용
        Notice deletedNotice = notice.markAsDeleted();
        noticeRepository.save(deletedNotice);
    }

    // 공지사항 수정
    @Transactional
    public void updateNotice(Long noticeId, NoticeRequest request, Admin admin) {
        if (admin == null) {
            throw new IllegalArgumentException("공지사항 수정 권한이 없습니다.");
        }
        
        System.out.println("=== 공지사항 수정 요청 ===");
        System.out.println("noticeId: " + noticeId);
        System.out.println("request.isPriority: " + request.isPriority());
        System.out.println("request.targetMode: " + request.getTargetMode());
        System.out.println("request.targetRoles: " + request.getTargetRoles());
        
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공지사항입니다."));

        System.out.println("기존 notice.isPriority: " + notice.isPriority());

        // 엔티티의 updateNotice 메서드 사용
        Notice updatedNotice = notice.updateNotice(
                request.getTitle(),
                request.getContent(),
                request.getCategory(),
                request.getTargetMode(),
                request.isPriority(),
                request.isPopup(),
                request.getPopupStartAt(),
                request.getPopupEndAt(),
                request.getStatus() != null ? request.getStatus() : notice.getStatus()
        );
        
        System.out.println("수정된 notice.isPriority: " + updatedNotice.isPriority());
        
        noticeRepository.save(updatedNotice);

        // 기존 타겟 권한 삭제 후 새로 저장 (여러 역할 지원)
        noticeTargetRoleRepository.deleteAllByNoticeId(noticeId);
        
        if (request.getTargetMode() == Notice.TargetMode.ROLE_SET && request.getTargetRoles() != null && !request.getTargetRoles().isEmpty()) {
            System.out.println("대상 역할 업데이트 시작... (총 " + request.getTargetRoles().size() + "개)");
            List<NoticeTargetRole> targetRoles = request.getTargetRoles().stream()
                    .map(role -> {
                        System.out.println("  - 역할 저장: " + role);
                        return NoticeTargetRole.builder()
                                .notice(updatedNotice)
                                .targetRole(role)
                                .build();
                    })
                    .collect(Collectors.toList());
            noticeTargetRoleRepository.saveAll(targetRoles);
            System.out.println("대상 역할 업데이트 완료");
        }

        // 기존 첨부파일 삭제 후 새로 저장
        if (request.getAttachments() != null) {
            noticeAttachmentRepository.deleteAllByNoticeId(noticeId);
            
            if (!request.getAttachments().isEmpty()) {
                System.out.println("첨부파일 업데이트 시작... (총 " + request.getAttachments().size() + "개)");
                List<NoticeAttachment> attachments = request.getAttachments().stream()
                        .map(dto -> NoticeAttachment.builder()
                                .notice(updatedNotice)
                                .fileName(dto.getFileName())
                                .originalFileName(dto.getOriginalFileName())
                                .filePath(dto.getFilePath())
                                .fileSize(dto.getFileSize())
                                .build())
                        .collect(Collectors.toList());
                noticeAttachmentRepository.saveAll(attachments);
                System.out.println("첨부파일 업데이트 완료");
            }
        }
        
        System.out.println("=== 공지사항 수정 완료 ===");
    }

    // 관리자용 목록 조회
    public Page<NoticeResponse> getAllNoticesForAdmin(Pageable pageable) {
        return noticeRepository.findAllByStatusNot(NoticeStatus.DELETED, pageable)
                .map(notice -> convertToResponse(notice, false));
    }

    // --- 사용자(User) 기능 ---

    // Req 64, 65: 사용자 권한별 목록 조회 + 중요공지 우선 (검색 기능 추가)
    public Page<NoticeResponse> getNoticesForUser(User user, String keyword, Pageable pageable) {
        Role role = (user != null) ? user.getRole() : null;
        
        System.out.println("=== NoticeService.getNoticesForUser 호출 ===");
        System.out.println("사용자 ID: " + (user != null ? user.getId() : "null"));
        System.out.println("사용자 역할: " + role);
        System.out.println("검색 키워드: " + keyword);
        
        // role이 null인 경우 빈 페이지 반환
        if (role == null) {
            System.out.println("경고: 사용자 역할이 null입니다. 빈 페이지를 반환합니다.");
            return Page.empty(pageable);
        }
        
        Page<Notice> notices = noticeRepository.findAllForUser(role, keyword, pageable);
        
        System.out.println("조회된 공지사항 수: " + notices.getContent().size());
        for (Notice notice : notices.getContent()) {
            System.out.println("=== 공지사항 상세 ===");
            System.out.println("  ID: " + notice.getId());
            System.out.println("  제목: " + notice.getTitle());
            System.out.println("  대상 모드: " + notice.getTargetMode());
            
            // 대상 역할 조회
            List<NoticeTargetRole> targetRoles = noticeTargetRoleRepository.findAllByNoticeId(notice.getId());
            System.out.println("  대상 역할 수: " + targetRoles.size());
            System.out.println("  대상 역할들: " + targetRoles.stream()
                    .map(ntr -> ntr.getTargetRole().toString())
                    .collect(Collectors.joining(", ")));
            
            // 현재 사용자 역할과 매칭 여부 확인
            boolean isMatch = notice.getTargetMode() == Notice.TargetMode.ALL ||
                    targetRoles.stream().anyMatch(ntr -> ntr.getTargetRole() == role);
            System.out.println("  현재 사용자 역할과 매칭: " + isMatch);
        }

        return notices.map(notice -> {
            boolean isRead = false;
            if (user != null) {
                isRead = noticeReadLogRepository.existsByNoticeIdAndUserId(notice.getId(), user.getId());
            }
            return convertToResponse(notice, isRead);
        });
    }

    // Req 67: 팝업 공지 조회
    public List<NoticeResponse> getActivePopupsForUser(User user) {
        LocalDateTime now = LocalDateTime.now();
        Role role = (user != null) ? user.getRole() : null;
        List<Notice> notices = noticeRepository.findActivePopups(role, now);

        return notices.stream()
                .map(notice -> {
                    boolean isRead = false;
                    if (user != null) {
                        isRead = noticeReadLogRepository.existsByNoticeIdAndUserId(notice.getId(), user.getId());
                    }
                    return convertToResponse(notice, isRead);
                })
                .collect(Collectors.toList());
    }

    // Req 69: 공지사항 필독 확인 (상세 조회 시 호출하거나, 별도 버튼 클릭 시 호출)
    @Transactional
    public void readNotice(Long noticeId, User user) {
        if (!noticeReadLogRepository.existsByNoticeIdAndUserId(noticeId, user.getId())) {
            Notice notice = noticeRepository.findById(noticeId)
                    .orElseThrow(() -> new IllegalArgumentException("공지사항 없음"));

            NoticeReadLog log = new NoticeReadLog(notice, user);
            noticeReadLogRepository.save(log);
        }
    }

    // 상세 조회
    public NoticeResponse getNoticeDetail(Long noticeId, User user) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 공지사항입니다."));

        boolean isRead = false;
        if (user != null) {
            isRead = noticeReadLogRepository.existsByNoticeIdAndUserId(noticeId, user.getId());
        }

        NoticeResponse response = convertToResponse(notice, isRead);

        // 이전글/다음글 ID 조회
        if (user != null) {
            noticeRepository.findPrevNoticeId(user.getRole(), noticeId)
                    .ifPresent(response::setPrevNoticeId);
            noticeRepository.findNextNoticeId(user.getRole(), noticeId)
                    .ifPresent(response::setNextNoticeId);
        }

        return response;
    }

    // Helper: Response 변환기
    private NoticeResponse convertToResponse(Notice notice, boolean isRead) {
        List<Role> roles = noticeTargetRoleRepository.findAllByNoticeId(notice.getId())
                .stream().map(NoticeTargetRole::getTargetRole).collect(Collectors.toList());

        List<NoticeAttachment> attachments = noticeAttachmentRepository.findAllByNoticeId(notice.getId());

        return NoticeResponse.from(notice, roles, attachments, isRead);
    }
}
