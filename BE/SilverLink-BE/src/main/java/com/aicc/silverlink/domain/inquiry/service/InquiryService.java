package com.aicc.silverlink.domain.inquiry.service;

// import com.aicc.silverlink.domain.assignment.entity.Assignment;
// import com.aicc.silverlink.domain.assignment.entity.Assignment.AssignmentStatus;
import com.aicc.silverlink.domain.assignment.repository.AssignmentRepository;
// import com.aicc.silverlink.domain.elderly.entity.Elderly;
import com.aicc.silverlink.domain.guardian.entity.GuardianElderly;
import com.aicc.silverlink.domain.guardian.repository.GuardianElderlyRepository;
import com.aicc.silverlink.domain.inquiry.dto.InquiryRequest;
import com.aicc.silverlink.domain.inquiry.dto.InquiryResponse;
import com.aicc.silverlink.domain.inquiry.entity.Inquiry;
import com.aicc.silverlink.domain.inquiry.entity.Inquiry.InquiryStatus;
import com.aicc.silverlink.domain.inquiry.entity.InquiryAnswer;
import com.aicc.silverlink.domain.inquiry.repository.InquiryAnswerRepository;
import com.aicc.silverlink.domain.inquiry.repository.InquiryRepository;
import com.aicc.silverlink.domain.notification.service.NotificationService;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.entity.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryService {

    private final InquiryRepository inquiryRepository;
    private final InquiryAnswerRepository inquiryAnswerRepository;
    private final GuardianElderlyRepository guardianElderlyRepository;
    private final AssignmentRepository assignmentRepository;
    private final NotificationService notificationService;

    // 문의 목록 조회
    public List<InquiryResponse> getInquiries(User user) {
        List<Inquiry> inquiries = Collections.emptyList();

        if (user.getRole() == Role.GUARDIAN) {
            // 보호자: 매핑된 어르신의 문의만 조회
            // 보호자: 매핑된 어르신의 문의만 조회
            inquiries = guardianElderlyRepository.findByGuardianId(user.getId())
                    .map(mapping -> inquiryRepository.findAllByElderlyIdAndIsDeletedFalseOrderByCreatedAtDesc(
                            mapping.getElderly().getId()))
                    .orElse(Collections.emptyList());

        } else if (user.getRole() == Role.COUNSELOR) {
            // 상담사: 배정된(ACTIVE) 어르신의 문의만 조회
            List<Long> elderlyIds = assignmentRepository.findAllActiveByCounselorId(user.getId())
                    .stream()
                    .map(assignment -> assignment.getElderly().getId())
                    .collect(Collectors.toList());

            if (!elderlyIds.isEmpty()) {
                inquiries = inquiryRepository.findAllByElderlyIdInAndIsDeletedFalseOrderByCreatedAtDesc(elderlyIds);
            }
        }

        return inquiries.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // 문의 상세 조회
    public InquiryResponse getInquiry(Long id, User user) {
        Inquiry inquiry = inquiryRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("문의를 찾을 수 없습니다."));

        validateReadPermission(inquiry, user);

        return toResponse(inquiry);
    }

    // 문의 등록 (보호자만 가능)
    @Transactional
    public InquiryResponse createInquiry(User user, InquiryRequest request) {
        if (user.getRole() != Role.GUARDIAN) {
            throw new IllegalArgumentException("보호자만 1:1 문의를 등록할 수 있습니다.");
        }

        GuardianElderly mapping = guardianElderlyRepository.findByGuardianId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("연결된 어르신 정보가 없습니다."));

        Inquiry inquiry = new Inquiry(mapping.getElderly(), user, request.getTitle(),
                request.getQuestionText());
        inquiryRepository.save(inquiry);

        return toResponse(inquiry);
    }

    // 문의 답변 등록 (상담사만 가능)
    @Transactional
    public void registerAnswer(Long inquiryId, User user, InquiryRequest request) {
        if (user.getRole() != Role.COUNSELOR) {
            throw new IllegalArgumentException("상담사만 답변을 등록할 수 있습니다.");
        }

        Inquiry inquiry = inquiryRepository.findByIdAndIsDeletedFalse(inquiryId)
                .orElseThrow(() -> new IllegalArgumentException("문의를 찾을 수 없습니다."));

        // 권한 체크 (담당 어르신인지)
        boolean isAssigned = assignmentRepository.existsByCounselorIdAndElderlyIdAndStatusActive(
                user.getId(), inquiry.getElderly().getId());

        if (!isAssigned) {
            throw new IllegalArgumentException("담당 어르신의 문의만 답변할 수 있습니다.");
        }

        InquiryAnswer answer = new InquiryAnswer(inquiry, user, request.getAnswerText());
        inquiryAnswerRepository.save(answer);

        inquiry.updateStatus(InquiryStatus.ANSWERED);

        // 문의 작성자에게 답변 알림 발송
        notificationService.createInquiryReplyNotification(
                inquiry.getCreatedBy().getId(),
                inquiryId,
                inquiry.getTitle());
    }

    // Helper to convert Entity to DTO
    private InquiryResponse toResponse(Inquiry inquiry) {
        InquiryAnswer answer = inquiryAnswerRepository.findByInquiryIdAndIsDeletedFalse(inquiry.getId()).orElse(null);
        return InquiryResponse.from(inquiry,
                answer != null ? answer.getAnswerText() : null,
                answer != null ? answer.getCreatedAt() : null);
    }

    private void validateReadPermission(Inquiry inquiry, User user) {
        if (user.getRole() == Role.GUARDIAN) {
            if (!inquiry.getCreatedBy().getId().equals(user.getId())) {
                // Or check if linked elderly is same? But simplest is own inquiry.
                // Requirement: "Guardians can view their own inquiries"
                throw new IllegalArgumentException("본인의 문의만 조회할 수 있습니다.");
            }
        } else if (user.getRole() == Role.COUNSELOR) {
            boolean isAssigned = assignmentRepository.existsByCounselorIdAndElderlyIdAndStatusActive(
                    user.getId(), inquiry.getElderly().getId());
            if (!isAssigned) {
                throw new IllegalArgumentException("담당 어르신의 문의만 조회할 수 있습니다.");
            }
        }
    }

}
