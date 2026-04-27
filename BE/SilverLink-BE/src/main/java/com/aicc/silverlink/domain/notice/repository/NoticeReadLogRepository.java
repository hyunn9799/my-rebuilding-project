package com.aicc.silverlink.domain.notice.repository;

import com.aicc.silverlink.domain.notice.entity.NoticeReadLog;
import com.aicc.silverlink.domain.notice.entity.NoticeReadLogId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoticeReadLogRepository extends JpaRepository<NoticeReadLog, NoticeReadLogId> {
    boolean existsByNoticeIdAndUserId(Long noticeId, Long userId);

    List<NoticeReadLog> findByNoticeId(Long noticeId);
}
