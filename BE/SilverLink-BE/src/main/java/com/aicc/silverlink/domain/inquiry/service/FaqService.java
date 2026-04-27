package com.aicc.silverlink.domain.inquiry.service;

import com.aicc.silverlink.domain.inquiry.dto.FaqResponse;
import com.aicc.silverlink.domain.inquiry.entity.Faq;
import com.aicc.silverlink.domain.inquiry.entity.Faq.FaqCategory;
import com.aicc.silverlink.domain.inquiry.repository.FaqRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FaqService {

    private final FaqRepository faqRepository;

    public List<FaqResponse> getFaqs(String category, String keyword) {
        List<Faq> faqs;

        if (keyword != null && !keyword.trim().isEmpty()) {
            faqs = faqRepository.findByQuestionContainingOrAnswerTextContainingAndIsActiveTrueOrderByDisplayOrderAsc(
                    keyword, keyword);
        } else if (category == null || category.trim().isEmpty() || "ALL".equalsIgnoreCase(category)) {
            faqs = faqRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc();
        } else {
            try {
                FaqCategory faqCategory = FaqCategory.valueOf(category.toUpperCase());
                faqs = faqRepository.findAllByCategoryAndIsActiveTrueOrderByDisplayOrderAsc(faqCategory);
            } catch (IllegalArgumentException e) {
                // Invalid category, return empty list or all? returning empty list is safer to
                // indicate no match
                return List.of();
            }
        }

        return faqs.stream()
                .map(FaqResponse::from)
                .collect(Collectors.toList());
    }
}
