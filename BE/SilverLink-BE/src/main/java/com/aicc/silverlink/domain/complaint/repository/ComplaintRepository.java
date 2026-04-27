package com.aicc.silverlink.domain.complaint.repository;

import com.aicc.silverlink.domain.complaint.entity.Complaint;
import com.aicc.silverlink.domain.complaint.entity.Complaint.ComplaintStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    // 작성자별 민원 목록 조회
    Page<Complaint> findByWriterId(Long writerId, Pageable pageable);

    // 작성자별 민원 목록 조회 (리스트)
    List<Complaint> findByWriterIdOrderByCreatedAtDesc(Long writerId);

    // 상태별 민원 조회
    Page<Complaint> findByStatus(ComplaintStatus status, Pageable pageable);

    // 상태별 민원 수 카운트
    long countByWriterIdAndStatus(Long writerId, ComplaintStatus status);

    // 전체 민원 수 카운트 (작성자별)
    long countByWriterId(Long writerId);
}
