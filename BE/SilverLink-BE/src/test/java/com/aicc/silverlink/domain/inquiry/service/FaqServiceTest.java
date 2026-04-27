package com.aicc.silverlink.domain.inquiry.service;

import com.aicc.silverlink.domain.inquiry.dto.FaqResponse;
import com.aicc.silverlink.domain.inquiry.entity.Faq;
import com.aicc.silverlink.domain.inquiry.entity.Faq.FaqCategory;
import com.aicc.silverlink.domain.inquiry.repository.FaqRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class FaqServiceTest {

    @InjectMocks
    private FaqService faqService;

    @Mock
    private FaqRepository faqRepository;

    @Test
    @DisplayName("전체 FAQ 목록 조회 (파라미터 없음)")
    void getFaqs_All() throws Exception {
        // given
        // new Faq() is protected, using reflection or helper
        Class<Faq> clazz = Faq.class;
        java.lang.reflect.Constructor<Faq> constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        Faq faq = constructor.newInstance();
        ReflectionTestUtils.setField(faq, "category", FaqCategory.SERVICE);
        ReflectionTestUtils.setField(faq, "question", "질문");
        ReflectionTestUtils.setField(faq, "answerText", "답변");

        given(faqRepository.findAllByIsActiveTrueOrderByDisplayOrderAsc())
                .willReturn(List.of(faq));

        // when
        List<FaqResponse> response = faqService.getFaqs(null, null);

        // then
        assertThat(response).hasSize(1);
        verify(faqRepository, times(1)).findAllByIsActiveTrueOrderByDisplayOrderAsc();
    }

    @Test
    @DisplayName("카테고리별 FAQ 목록 조회")
    void getFaqs_Category() throws Exception {
        // given
        // new Faq() is protected, using reflection or helper
        Class<Faq> clazz = Faq.class;
        java.lang.reflect.Constructor<Faq> constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        Faq faq = constructor.newInstance();
        ReflectionTestUtils.setField(faq, "category", FaqCategory.WELFARE);
        ReflectionTestUtils.setField(faq, "question", "복지 질문");
        ReflectionTestUtils.setField(faq, "answerText", "복지 답변");

        given(faqRepository.findAllByCategoryAndIsActiveTrueOrderByDisplayOrderAsc(FaqCategory.WELFARE))
                .willReturn(List.of(faq));

        // when
        List<FaqResponse> response = faqService.getFaqs("WELFARE", null);

        // then
        assertThat(response).hasSize(1);
        assertThat(response.get(0).getQuestion()).isEqualTo("복지 질문");
        verify(faqRepository, times(1)).findAllByCategoryAndIsActiveTrueOrderByDisplayOrderAsc(FaqCategory.WELFARE);
    }

    @Test
    @DisplayName("키워드 검색 FAQ 목록 조회")
    void getFaqs_Keyword() throws Exception {
        // given
        String keyword = "챗봇";
        // new Faq() is protected, using reflection or helper
        Class<Faq> clazz = Faq.class;
        java.lang.reflect.Constructor<Faq> constructor = clazz.getDeclaredConstructor();
        constructor.setAccessible(true);
        Faq faq = constructor.newInstance();
        ReflectionTestUtils.setField(faq, "category", FaqCategory.SERVICE);
        ReflectionTestUtils.setField(faq, "question", "챗봇 질문");
        ReflectionTestUtils.setField(faq, "answerText", "챗봇 답변");

        given(faqRepository.findByQuestionContainingOrAnswerTextContainingAndIsActiveTrueOrderByDisplayOrderAsc(keyword,
                keyword))
                .willReturn(List.of(faq));

        // when
        List<FaqResponse> response = faqService.getFaqs(null, keyword);

        // then
        assertThat(response).hasSize(1);
        verify(faqRepository, times(1))
                .findByQuestionContainingOrAnswerTextContainingAndIsActiveTrueOrderByDisplayOrderAsc(keyword, keyword);
    }

    @Test
    @DisplayName("유효하지 않은 카테고리로 조회 시 빈 목록 반환")
    void getFaqs_InvalidCategory() {
        // when
        List<FaqResponse> response = faqService.getFaqs("INVALID_CATEGORY", null);

        // then
        assertThat(response).isEmpty();
        verify(faqRepository, times(0)).findAllByCategoryAndIsActiveTrueOrderByDisplayOrderAsc(any());
        verify(faqRepository, times(0)).findAllByIsActiveTrueOrderByDisplayOrderAsc();
    }
}
