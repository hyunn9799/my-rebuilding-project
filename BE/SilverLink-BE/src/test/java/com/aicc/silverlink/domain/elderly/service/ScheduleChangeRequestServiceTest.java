package com.aicc.silverlink.domain.elderly.service;

import com.aicc.silverlink.domain.elderly.dto.ScheduleChangeRequestDto.*;
import com.aicc.silverlink.domain.elderly.entity.CallScheduleHistory;
import com.aicc.silverlink.domain.elderly.entity.Elderly;
import com.aicc.silverlink.domain.elderly.entity.ScheduleChangeRequest;
import com.aicc.silverlink.domain.elderly.entity.ScheduleChangeRequest.RequestStatus;
import com.aicc.silverlink.domain.elderly.repository.CallScheduleHistoryRepository;
import com.aicc.silverlink.domain.elderly.repository.ElderlyRepository;
import com.aicc.silverlink.domain.elderly.repository.ScheduleChangeRequestRepository;
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.repository.UserRepository;
import com.aicc.silverlink.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduleChangeRequestService 테스트")
class ScheduleChangeRequestServiceTest {

    @Mock
    private ScheduleChangeRequestRepository changeRequestRepository;

    @Mock
    private ElderlyRepository elderlyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CallScheduleHistoryRepository historyRepository;

    @InjectMocks
    private ScheduleChangeRequestService service;

    private User mockElderlyUser;
    private User mockCounselor;
    private Elderly mockElderly;
    private ScheduleChangeRequest mockRequest;

    @BeforeEach
    void setUp() {
        mockElderlyUser = mock(User.class);
        lenient().when(mockElderlyUser.getId()).thenReturn(1L);
        lenient().when(mockElderlyUser.getName()).thenReturn("홍길동");

        mockCounselor = mock(User.class);
        lenient().when(mockCounselor.getId()).thenReturn(100L);
        lenient().when(mockCounselor.getName()).thenReturn("상담사");
        lenient().when(mockCounselor.getRole()).thenReturn(Role.COUNSELOR);

        mockElderly = mock(Elderly.class);
        lenient().when(mockElderly.getId()).thenReturn(1L);
        lenient().when(mockElderly.getUser()).thenReturn(mockElderlyUser);
        lenient().when(mockElderly.getPreferredCallTime()).thenReturn("09:00");
        lenient().when(mockElderly.getPreferredCallDays()).thenReturn("MON,WED,FRI");
        lenient().when(mockElderly.getCallScheduleEnabled()).thenReturn(true);

        mockRequest = mock(ScheduleChangeRequest.class);
        lenient().when(mockRequest.getId()).thenReturn(1L);
        lenient().when(mockRequest.getElderly()).thenReturn(mockElderly);
        lenient().when(mockRequest.getRequestedCallTime()).thenReturn("14:00");
        lenient().when(mockRequest.getRequestedCallDays()).thenReturn("TUE,THU");
        lenient().when(mockRequest.getStatus()).thenReturn(RequestStatus.PENDING);
    }

    @Nested
    @DisplayName("createRequest - 변경 요청 생성")
    class CreateRequestTest {

        @Test
        @DisplayName("변경 요청 생성 성공")
        void createRequest_success() {
            // given
            given(changeRequestRepository.existsByElderlyIdAndStatus(1L, RequestStatus.PENDING))
                    .willReturn(false);
            given(elderlyRepository.findById(1L)).willReturn(Optional.of(mockElderly));

            CreateRequest request = new CreateRequest();
            request.setPreferredCallTime("14:00");
            request.setPreferredCallDays(List.of("TUE", "THU"));

            // when
            service.createRequest(1L, request);

            // then
            verify(changeRequestRepository).save(any(ScheduleChangeRequest.class));
        }

        @Test
        @DisplayName("이미 대기 중인 요청이 있으면 예외 발생")
        void createRequest_duplicatePending() {
            // given
            given(changeRequestRepository.existsByElderlyIdAndStatus(1L, RequestStatus.PENDING))
                    .willReturn(true);

            CreateRequest request = new CreateRequest();

            // when & then
            assertThatThrownBy(() -> service.createRequest(1L, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("이미 대기 중인 변경 요청");
        }
    }

    @Nested
    @DisplayName("approveRequest - 요청 승인")
    class ApproveRequestTest {

        @Test
        @DisplayName("요청 승인 시 스케줄 업데이트 및 이력 저장")
        void approveRequest_success() {
            // given
            given(changeRequestRepository.findById(1L)).willReturn(Optional.of(mockRequest));
            given(userRepository.findById(100L)).willReturn(Optional.of(mockCounselor));

            // when
            service.approveRequest(1L, 100L);

            // then
            verify(mockRequest).approve(mockCounselor);
            verify(mockElderly).updateCallSchedule("14:00", "TUE,THU", true);

            ArgumentCaptor<CallScheduleHistory> historyCaptor = ArgumentCaptor.forClass(CallScheduleHistory.class);
            verify(historyRepository).save(historyCaptor.capture());

            CallScheduleHistory savedHistory = historyCaptor.getValue();
            assertThat(savedHistory.getChangeType()).isEqualTo(CallScheduleHistory.ChangeType.REQUEST_APPROVED);
            assertThat(savedHistory.getRelatedRequestId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("이미 처리된 요청 승인 시 예외 발생")
        void approveRequest_alreadyProcessed() {
            // given
            when(mockRequest.getStatus()).thenReturn(RequestStatus.APPROVED);
            given(changeRequestRepository.findById(1L)).willReturn(Optional.of(mockRequest));

            // when & then
            assertThatThrownBy(() -> service.approveRequest(1L, 100L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("이미 처리된 요청");
        }
    }

    @Nested
    @DisplayName("rejectRequest - 요청 거절")
    class RejectRequestTest {

        @Test
        @DisplayName("요청 거절 시 이력에 사유 저장")
        void rejectRequest_success() {
            // given
            given(changeRequestRepository.findById(1L)).willReturn(Optional.of(mockRequest));
            given(userRepository.findById(100L)).willReturn(Optional.of(mockCounselor));

            RejectRequest rejectRequest = new RejectRequest();
            rejectRequest.setReason("시간대가 상담 불가 시간입니다");

            // when
            service.rejectRequest(1L, 100L, rejectRequest);

            // then
            verify(mockRequest).reject(mockCounselor, "시간대가 상담 불가 시간입니다");

            ArgumentCaptor<CallScheduleHistory> historyCaptor = ArgumentCaptor.forClass(CallScheduleHistory.class);
            verify(historyRepository).save(historyCaptor.capture());

            CallScheduleHistory savedHistory = historyCaptor.getValue();
            assertThat(savedHistory.getChangeType()).isEqualTo(CallScheduleHistory.ChangeType.REQUEST_REJECTED);
            assertThat(savedHistory.getChangeReason()).isEqualTo("시간대가 상담 불가 시간입니다");
        }
    }

    @Nested
    @DisplayName("getPendingRequests - 대기 요청 목록")
    class GetPendingRequestsTest {

        @Test
        @DisplayName("대기 중인 요청 목록 조회")
        void getPendingRequests_success() {
            // given
            given(changeRequestRepository.findPendingRequestsWithElderly(RequestStatus.PENDING))
                    .willReturn(List.of(mockRequest));

            // when
            List<Response> result = service.getPendingRequests();

            // then
            assertThat(result).hasSize(1);
            verify(changeRequestRepository).findPendingRequestsWithElderly(RequestStatus.PENDING);
        }
    }
}
