package com.aicc.silverlink.domain.notice.repository;

import com.aicc.silverlink.domain.notice.entity.NoticeTargetRole;
import com.aicc.silverlink.domain.notice.entity.NoticeTargetRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface NoticeTargetRoleRepository extends JpaRepository<NoticeTargetRole, NoticeTargetRoleId> {

    // 특정 공지사항에 설정된 모든 타겟 권한 목록을 가져옵니다.
    List<NoticeTargetRole> findAllByNoticeId(Long noticeId);

    // 수정 시 기존 권한 설정을 삭제하기 위해 필요합니다.
    @Modifying
    @Transactional
    @Query("DELETE FROM NoticeTargetRole ntr WHERE ntr.notice.id = :noticeId")
    void deleteAllByNoticeId(@Param("noticeId") Long noticeId);
}