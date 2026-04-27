package com.aicc.silverlink.domain.inquiry.repository;

import com.aicc.silverlink.domain.inquiry.entity.Faq;
import com.aicc.silverlink.domain.inquiry.entity.Faq.FaqCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FaqRepository extends JpaRepository<Faq, Long> {
    List<Faq> findAllByCategoryAndIsActiveTrueOrderByDisplayOrderAsc(FaqCategory category);

    List<Faq> findAllByIsActiveTrueOrderByDisplayOrderAsc();

    List<Faq> findByQuestionContainingOrAnswerTextContainingAndIsActiveTrueOrderByDisplayOrderAsc(
            String questionKeyword, String answerKeyword);

    /**
     * 특정 시간 이후 업데이트된 활성 FAQ 조회 (Python 챗봇 증분 동기화용)
     */
    List<Faq> findAllByIsActiveTrueAndUpdatedAtAfterOrderByUpdatedAtAsc(LocalDateTime since);
}
