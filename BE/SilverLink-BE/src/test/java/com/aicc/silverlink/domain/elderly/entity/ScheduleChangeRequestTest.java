package com.aicc.silverlink.domain.elderly.entity;

import com.aicc.silverlink.domain.elderly.entity.ScheduleChangeRequest.RequestStatus;
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * ScheduleChangeRequest 엔티티 단위 테스트
 */
@DisplayName("ScheduleChangeRequest 엔티티 테스트")
class ScheduleChangeRequestTest {

    private Elderly mockElderly;
    private User mockCounselor;

    @BeforeEach
    void setUp() {
        User mockUser = mock(User.class);
        when(mockUser.getId()).thenReturn(1L);
        when(mockUser.getName()).thenReturn("홍길동");

        mockElderly = mock(Elderly.class);
        when(mockElderly.getId()).thenReturn(1L);
        when(mockElderly.getUser()).thenReturn(mockUser);

        mockCounselor = mock(User.class);
        when(mockCounselor.getId()).thenReturn(100L);
        when(mockCounselor.getName()).thenReturn("상담사");
        when(mockCounselor.getRole()).thenReturn(Role.COUNSELOR);
    }

    @Nested
    @DisplayName("create - 정적 팩토리 메서드")
    class CreateTest {

        @Test
        @DisplayName("변경 요청 생성 성공")
        void create_success() {
            // when
            ScheduleChangeRequest request = ScheduleChangeRequest.create(
                    mockElderly,
                    "14:00",
                    "TUE,THU");

            // then
            assertThat(request.getElderly()).isEqualTo(mockElderly);
            assertThat(request.getRequestedCallTime()).isEqualTo("14:00");
            assertThat(request.getRequestedCallDays()).isEqualTo("TUE,THU");
            assertThat(request.getStatus()).isEqualTo(RequestStatus.PENDING);
            assertThat(request.getProcessedAt()).isNull();
            assertThat(request.getProcessedBy()).isNull();
            assertThat(request.getRejectReason()).isNull();
        }

        @Test
        @DisplayName("생성 시 기본 상태는 PENDING")
        void create_defaultStatusIsPending() {
            // when
            ScheduleChangeRequest request = ScheduleChangeRequest.create(
                    mockElderly,
                    "09:00",
                    "MON,WED,FRI");

            // then
            assertThat(request.getStatus()).isEqualTo(RequestStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("approve - 요청 승인")
    class ApproveTest {

        @Test
        @DisplayName("요청 승인 시 상태와 처리 정보 업데이트")
        void approve_success() {
            // given
            ScheduleChangeRequest request = ScheduleChangeRequest.create(
                    mockElderly,
                    "14:00",
                    "TUE,THU");

            // when
            request.approve(mockCounselor);

            // then
            assertThat(request.getStatus()).isEqualTo(RequestStatus.APPROVED);
            assertThat(request.getProcessedBy()).isEqualTo(mockCounselor);
            assertThat(request.getProcessedAt()).isNotNull();
            assertThat(request.getRejectReason()).isNull();
        }
    }

    @Nested
    @DisplayName("reject - 요청 거절")
    class RejectTest {

        @Test
        @DisplayName("요청 거절 시 상태, 처리 정보, 거절 사유 업데이트")
        void reject_success() {
            // given
            ScheduleChangeRequest request = ScheduleChangeRequest.create(
                    mockElderly,
                    "14:00",
                    "TUE,THU");

            // when
            request.reject(mockCounselor, "업무 시간 외 통화 요청");

            // then
            assertThat(request.getStatus()).isEqualTo(RequestStatus.REJECTED);
            assertThat(request.getProcessedBy()).isEqualTo(mockCounselor);
            assertThat(request.getProcessedAt()).isNotNull();
            assertThat(request.getRejectReason()).isEqualTo("업무 시간 외 통화 요청");
        }

        @Test
        @DisplayName("거절 사유 없이 거절 가능")
        void reject_withoutReason() {
            // given
            ScheduleChangeRequest request = ScheduleChangeRequest.create(
                    mockElderly,
                    "14:00",
                    "TUE,THU");

            // when
            request.reject(mockCounselor, null);

            // then
            assertThat(request.getStatus()).isEqualTo(RequestStatus.REJECTED);
            assertThat(request.getRejectReason()).isNull();
        }
    }

    @Nested
    @DisplayName("RequestStatus Enum 테스트")
    class RequestStatusTest {

        @Test
        @DisplayName("Enum values 확인")
        void requestStatus_values() {
            RequestStatus[] statuses = RequestStatus.values();
            assertThat(statuses).hasSize(3);
            assertThat(statuses).containsExactly(
                    RequestStatus.PENDING,
                    RequestStatus.APPROVED,
                    RequestStatus.REJECTED);
        }

        @Test
        @DisplayName("valueOf 동작 확인")
        void requestStatus_valueOf() {
            assertThat(RequestStatus.valueOf("PENDING")).isEqualTo(RequestStatus.PENDING);
            assertThat(RequestStatus.valueOf("APPROVED")).isEqualTo(RequestStatus.APPROVED);
            assertThat(RequestStatus.valueOf("REJECTED")).isEqualTo(RequestStatus.REJECTED);
        }
    }
}
