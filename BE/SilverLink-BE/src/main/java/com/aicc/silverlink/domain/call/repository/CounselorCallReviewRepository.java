package com.aicc.silverlink.domain.call.repository;

import com.aicc.silverlink.domain.call.entity.CounselorCallReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CounselorCallReviewRepository extends JpaRepository<CounselorCallReview, Long> {

    /**
     * 통화 ID와 상담사 ID로 리뷰 조회
     */
    Optional<CounselorCallReview> findByCallRecordIdAndCounselorId(Long callId, Long counselorId);

    /**
     * 통화 ID로 리뷰 존재 여부 확인
     */
    boolean existsByCallRecordId(Long callId);

    /**
     * 통화 ID와 상담사 ID로 리뷰 존재 여부 확인
     */
    boolean existsByCallRecordIdAndCounselorId(Long callId, Long counselorId);

    /**
     * 통화 ID로 모든 리뷰 조회
     */
    List<CounselorCallReview> findByCallRecordIdOrderByReviewedAtDesc(Long callId);

    /**
     * 상담사의 리뷰 목록 조회
     */
    Page<CounselorCallReview> findByCounselorIdOrderByReviewedAtDesc(Long counselorId, Pageable pageable);

    /**
     * 긴급 리뷰 목록 조회
     */
    @Query("SELECT r FROM CounselorCallReview r WHERE r.urgent = true ORDER BY r.reviewedAt DESC")
    Page<CounselorCallReview> findUrgentReviews(Pageable pageable);

    /**
     * 어르신의 통화에 대한 리뷰 조회 (보호자 조회용)
     */
    @Query("SELECT r FROM CounselorCallReview r " +
            "JOIN r.callRecord c " +
            "WHERE c.elderly.id = :elderlyId " +
            "ORDER BY r.reviewedAt DESC")
    Page<CounselorCallReview> findReviewsByElderlyId(@Param("elderlyId") Long elderlyId, Pageable pageable);

    /**
     * 어르신의 최근 리뷰 조회
     */
    @Query("SELECT r FROM CounselorCallReview r " +
            "JOIN r.callRecord c " +
            "WHERE c.elderly.id = :elderlyId " +
            "ORDER BY r.reviewedAt DESC")
    List<CounselorCallReview> findRecentReviewsByElderlyId(@Param("elderlyId") Long elderlyId, Pageable pageable);
}