package com.aicc.silverlink.domain.inquiry.repository;

import com.aicc.silverlink.domain.inquiry.entity.InquiryAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InquiryAnswerRepository extends JpaRepository<InquiryAnswer, Long> {
    Optional<InquiryAnswer> findByInquiryIdAndIsDeletedFalse(Long inquiryId);
}
