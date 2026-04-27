package com.aicc.silverlink.domain.inquiry.service;

import com.aicc.silverlink.domain.assignment.entity.Assignment;
import com.aicc.silverlink.domain.assignment.repository.AssignmentRepository;
import com.aicc.silverlink.domain.elderly.entity.Elderly;
import com.aicc.silverlink.domain.guardian.entity.GuardianElderly;
import com.aicc.silverlink.domain.guardian.repository.GuardianElderlyRepository;
import com.aicc.silverlink.domain.inquiry.dto.InquiryResponse;
import com.aicc.silverlink.domain.inquiry.entity.Inquiry;
import com.aicc.silverlink.domain.inquiry.repository.InquiryAnswerRepository;
import com.aicc.silverlink.domain.inquiry.repository.InquiryRepository;
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.domain.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InquiryServiceTest {

    @Mock
    private InquiryRepository inquiryRepository;
    @Mock
    private InquiryAnswerRepository inquiryAnswerRepository;
    @Mock
    private GuardianElderlyRepository guardianElderlyRepository;
    @Mock
    private AssignmentRepository assignmentRepository;

    @InjectMocks
    private InquiryService inquiryService;

    @Test
    void getInquiries_Guardian_ShouldReturnMappedInquiries() {
        // [Given]
        User guardian = mock(User.class);
        given(guardian.getRole()).willReturn(Role.GUARDIAN);
        given(guardian.getId()).willReturn(1L);

        GuardianElderly mapping = mock(GuardianElderly.class);
        Elderly elderly = mock(Elderly.class);
        given(mapping.getElderly()).willReturn(elderly);
        given(elderly.getId()).willReturn(10L);

        given(guardianElderlyRepository.findByGuardianId(1L)).willReturn(Optional.of(mapping));

        Inquiry inquiry = mock(Inquiry.class); // Mock Inquiry for simplicity
        given(inquiry.getId()).willReturn(100L);
        given(inquiry.getElderly()).willReturn(elderly);
        User userMock = mock(User.class);
        given(userMock.getName()).willReturn("TestUser");
        given(elderly.getUser()).willReturn(userMock);

        given(inquiryRepository.findAllByElderlyIdAndIsDeletedFalseOrderByCreatedAtDesc(10L))
                .willReturn(List.of(inquiry));

        // [When]
        List<InquiryResponse> results = inquiryService.getInquiries(guardian);

        // [Then]
        assertThat(results).hasSize(1);
        verify(guardianElderlyRepository).findByGuardianId(1L);
    }
}
