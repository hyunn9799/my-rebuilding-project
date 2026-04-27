package com.aicc.silverlink.domain.audit.service;

import com.aicc.silverlink.domain.audit.entity.AuditLog;
import com.aicc.silverlink.domain.audit.repository.AuditLogRepository;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * [비동기] 감사 로그 기록
     * - @Async: 메인 비즈니스 로직(예: 약관 생성)을 기다리게 하지 않고 별도 스레드에서 실행
     * - REQUIRES_NEW: 메인 트랜잭션이 롤백되어도 로그 저장은 성공하도록 분리 (선택 사항, 보통 로그는 남기는 편)
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLog(Long actorId, String action, String targetEntity, Long targetId, String clientIp,
            String meta) {
        try {
            // actorId가 null이면 시스템(System) 또는 비회원
            // getReferenceById: DB 조회를 하지 않고 프록시 객체만 가져옴 (성능 최적화)
            User actor = (actorId != null) ? userRepository.getReferenceById(actorId) : null;

            String validJsonMeta = meta;
            if (meta != null && !meta.isBlank()) {
                try {
                    // 이미 JSON 형식이면 통과
                    objectMapper.readTree(meta);
                } catch (Exception e) {
                    // JSON이 아니면 객체로 감싸기
                    validJsonMeta = objectMapper.writeValueAsString(Collections.singletonMap("description", meta));
                }
            } else {
                // null이나 빈 문자열이면 빈 JSON 객체로
                validJsonMeta = "{}";
            }

            AuditLog auditLog = AuditLog.builder()
                    .actor(actor)
                    .action(action)
                    .targetEntity(targetEntity)
                    .targetId(targetId)
                    .ipAddress(clientIp)
                    .meta(validJsonMeta)
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit Log recorded: [{}] Action by User ID {}", action, actorId);

        } catch (Exception e) {
            // 로그 저장이 실패했다고 해서 메인 비즈니스 로직을 멈추거나 롤백시키면 안 됨
            log.error("감사 로그 저장 실패 (데이터 손실 가능성 있음): ", e);
        }
    }

    /**
     * 감사 로그 조회 (관리자용)
     * - 읽기 전용 트랜잭션
     */
    @Transactional(readOnly = true)
    public Page<AuditLog> findAllLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }
}