package com.aicc.silverlink.domain.complaint.service;

import com.aicc.silverlink.domain.admin.entity.Admin;
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.domain.complaint.dto.ComplaintRequest;
import com.aicc.silverlink.domain.complaint.dto.ComplaintResponse;
import com.aicc.silverlink.domain.complaint.entity.Complaint;
import com.aicc.silverlink.domain.complaint.entity.Complaint.ComplaintStatus;
import com.aicc.silverlink.domain.complaint.repository.ComplaintRepository;
import com.aicc.silverlink.domain.notification.service.NotificationService;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    /**
     * 민원 등록 (보호자)
     */
    @Transactional
    public ComplaintResponse createComplaint(Long userId, ComplaintRequest request) {
        User writer = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Complaint complaint = Complaint.builder()
                .writer(writer)
                .title(request.getTitle())
                .content(request.getContent())
                .build();

        Complaint saved = complaintRepository.save(complaint);

        // 모든 관리자에게 알림 발송
        List<User> admins = userRepository.findByRoleIn(List.of(Role.ADMIN));
        for (User admin : admins) {
            notificationService.createComplaintNewNotification(
                    admin.getId(),
                    saved.getId(),
                    writer.getName(),
                    saved.getTitle());
        }

        return ComplaintResponse.from(saved);
    }

    /**
     * 내 민원 목록 조회 (보호자)
     */
    public List<ComplaintResponse> getMyComplaints(Long userId) {
        return complaintRepository.findByWriterIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(ComplaintResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 민원 상세 조회
     */
    public ComplaintResponse getComplaintDetail(Long complaintId, Long userId) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("민원을 찾을 수 없습니다."));

        // 작성자 본인인지 확인
        if (!complaint.getWriter().getId().equals(userId)) {
            throw new IllegalArgumentException("조회 권한이 없습니다.");
        }

        return ComplaintResponse.from(complaint);
    }

    /**
     * 민원 통계 조회 (보호자)
     */
    public Map<String, Long> getMyComplaintStats(Long userId) {
        long pending = complaintRepository.countByWriterIdAndStatus(userId, ComplaintStatus.WAITING);
        long processing = complaintRepository.countByWriterIdAndStatus(userId, ComplaintStatus.PROCESSING);
        long resolved = complaintRepository.countByWriterIdAndStatus(userId, ComplaintStatus.RESOLVED);
        long total = complaintRepository.countByWriterId(userId);

        return Map.of(
                "pending", pending,
                "processing", processing,
                "resolved", resolved,
                "total", total);
    }

    /**
     * 전체 민원 목록 조회 (관리자)
     */
    public Page<ComplaintResponse> getAllComplaints(Pageable pageable) {
        return complaintRepository.findAll(pageable)
                .map(ComplaintResponse::from);
    }

    /**
     * 상태별 민원 조회 (관리자)
     */
    public Page<ComplaintResponse> getComplaintsByStatus(ComplaintStatus status, Pageable pageable) {
        return complaintRepository.findByStatus(status, pageable)
                .map(ComplaintResponse::from);
    }

    /**
     * 민원 답변 (관리자)
     */
    @Transactional
    public ComplaintResponse replyToComplaint(Long complaintId, String replyContent, Admin admin) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("민원을 찾을 수 없습니다."));

        complaint.reply(replyContent, admin);

        // 민원 작성자에게 답변 알림 발송
        notificationService.createComplaintReplyNotification(
                complaint.getWriter().getId(),
                complaintId,
                complaint.getTitle());

        return ComplaintResponse.from(complaint);
    }

    /**
     * 민원 상태 변경 (관리자)
     */
    @Transactional
    public ComplaintResponse updateComplaintStatus(Long complaintId, ComplaintStatus newStatus) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new IllegalArgumentException("민원을 찾을 수 없습니다."));

        complaint.updateStatus(newStatus);
        return ComplaintResponse.from(complaint);
    }

}
