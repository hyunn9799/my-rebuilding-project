package com.aicc.silverlink.domain.notice.repository;

import com.aicc.silverlink.domain.notice.entity.Notice;
import com.aicc.silverlink.domain.notice.entity.Notice.NoticeStatus;
import com.aicc.silverlink.domain.user.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {

    // 관리자용: 삭제되지 않은 모든 공지 조회 (중요공지 우선 정렬)
    @Query("SELECT n FROM Notice n " +
            "WHERE n.status != 'DELETED' " +
            "ORDER BY n.isPriority DESC, n.createdAt DESC")
    Page<Notice> findAllByStatusNot(NoticeStatus status, Pageable pageable);

    // Req 64, 65: 사용자용 공지 목록 조회 (검색 기능 포함)
    // targetMode가 ALL인 경우와 ROLE_SET인 경우를 명확히 분리
    @Query("SELECT DISTINCT n FROM Notice n " +
            "LEFT JOIN NoticeTargetRole ntr ON n.id = ntr.notice.id " +
            "WHERE n.status = 'PUBLISHED' " +
            "AND n.deletedAt IS NULL " +
            "AND (" +
            "  n.targetMode = 'ALL' " +
            "  OR (n.targetMode = 'ROLE_SET' AND ntr.targetRole = :role)" +
            ") " +
            "AND (:keyword IS NULL OR :keyword = '' OR n.title LIKE %:keyword% OR n.content LIKE %:keyword%) " +
            "ORDER BY n.isPriority DESC, n.createdAt DESC")
    Page<Notice> findAllForUser(@Param("role") Role role, @Param("keyword") String keyword, Pageable pageable);

    // Req 67: 유효한 팝업 공지 조회
    @Query("SELECT DISTINCT n FROM Notice n " +
            "LEFT JOIN NoticeTargetRole ntr ON n.id = ntr.notice.id " +
            "WHERE n.status = 'PUBLISHED' " +
            "AND n.deletedAt IS NULL " +
            "AND n.isPopup = true " +
            "AND :now BETWEEN n.popupStartAt AND n.popupEndAt " +
            "AND (n.targetMode = 'ALL' OR (n.targetMode = 'ROLE_SET' AND ntr.targetRole = :role))")
    List<Notice> findActivePopups(@Param("role") Role role, @Param("now") LocalDateTime now);

    // 이전 글 조회 (현재 ID보다 작은 ID 중 가장 큰 값)
    @Query(value = "SELECT n.notice_id FROM notices n " +
            "LEFT JOIN notice_target_roles ntr ON n.notice_id = ntr.notice_id " +
            "WHERE n.status = 'PUBLISHED' " +
            "AND n.deleted_at IS NULL " +
            "AND (n.target_mode = 'ALL' OR ntr.target_role = :#{#role.name()}) " +
            "AND n.notice_id < :currentId " +
            "ORDER BY n.notice_id DESC LIMIT 1", nativeQuery = true)
    Optional<Long> findPrevNoticeId(@Param("role") Role role, @Param("currentId") Long currentId);

    // 다음 글 조회 (현재 ID보다 큰 ID 중 가장 작은 값)
    @Query(value = "SELECT n.notice_id FROM notices n " +
            "LEFT JOIN notice_target_roles ntr ON n.notice_id = ntr.notice_id " +
            "WHERE n.status = 'PUBLISHED' " +
            "AND n.deleted_at IS NULL " +
            "AND (n.target_mode = 'ALL' OR ntr.target_role = :#{#role.name()}) " +
            "AND n.notice_id > :currentId " +
            "ORDER BY n.notice_id ASC LIMIT 1", nativeQuery = true)
    Optional<Long> findNextNoticeId(@Param("role") Role role, @Param("currentId") Long currentId);
}
