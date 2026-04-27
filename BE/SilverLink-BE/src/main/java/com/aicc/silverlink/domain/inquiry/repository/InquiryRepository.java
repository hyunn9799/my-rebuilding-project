package com.aicc.silverlink.domain.inquiry.repository;

import com.aicc.silverlink.domain.inquiry.entity.Inquiry;
import com.aicc.silverlink.domain.inquiry.entity.Inquiry.InquiryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
    List<Inquiry> findAllByElderlyIdAndIsDeletedFalseOrderByCreatedAtDesc(Long elderlyId);

    List<Inquiry> findAllByElderlyIdInAndIsDeletedFalseOrderByCreatedAtDesc(List<Long> elderlyIds);

    Optional<Inquiry> findByIdAndIsDeletedFalse(Long id);

    /**
     * 답변 완료된 모든 문의 조회 (Python 챗봇 동기화용)
     */
    List<Inquiry> findAllByStatusAndIsDeletedFalseOrderByCreatedAtDesc(InquiryStatus status);

    /**
     * 특정 보호자-어르신 관계의 답변 완료된 문의 조회 (Python 챗봇 권한 필터링용)
     */
    @Query("SELECT i FROM Inquiry i " +
            "WHERE i.elderly.id = :elderlyId " +
            "AND i.createdBy.id = :guardianId " +
            "AND i.status = 'ANSWERED' " +
            "AND i.isDeleted = false " +
            "ORDER BY i.createdAt DESC")
    List<Inquiry> findAnsweredInquiriesByRelation(
            @Param("guardianId") Long guardianId,
            @Param("elderlyId") Long elderlyId);
}
