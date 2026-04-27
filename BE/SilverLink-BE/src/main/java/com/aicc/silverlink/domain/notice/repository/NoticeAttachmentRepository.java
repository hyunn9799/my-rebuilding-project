package com.aicc.silverlink.domain.notice.repository;

import com.aicc.silverlink.domain.notice.entity.NoticeAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface NoticeAttachmentRepository extends JpaRepository<NoticeAttachment, Long> {

    // 특정 공지사항에 속한 첨부파일 목록을 가져옵니다.
    List<NoticeAttachment> findAllByNoticeId(Long noticeId);

    // 특정 공지사항의 첨부파일 모두 삭제
    @Modifying
    @Transactional
    @Query("DELETE FROM NoticeAttachment na WHERE na.notice.id = :noticeId")
    void deleteAllByNoticeId(@Param("noticeId") Long noticeId);
}
