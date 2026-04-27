package com.aicc.silverlink.domain.elderly.entity;

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
 * CallScheduleHistory 엔티티 단위 테스트
 */
@DisplayName("CallScheduleHistory 엔티티 테스트")
class CallScheduleHistoryTest {

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
    @DisplayName("createDirectUpdate - 직접 수정 이력 생성")
    class CreateDirectUpdateTest {

        @Test
        @DisplayName("직접 수정 이력 생성 성공")
        void createDirectUpdate_success() {
            // when
            CallScheduleHistory history = CallScheduleHistory.createDirectUpdate(
                    mockElderly,
                    mockCounselor,
                    "09:00", "MON,WED,FRI", true,
                    "14:00", "TUE,THU", true,
                    "어르신 구두 요청");

            // then
            assertThat(history.getElderly()).isEqualTo(mockElderly);
            assertThat(history.getChangedBy()).isEqualTo(mockCounselor);
            assertThat(history.getChangeType()).isEqualTo(CallScheduleHistory.ChangeType.DIRECT_UPDATE);
            assertThat(history.getPreviousTime()).isEqualTo("09:00");
            assertThat(history.getPreviousDays()).isEqualTo("MON,WED,FRI");
            assertThat(history.getPreviousEnabled()).isTrue();
            assertThat(history.getNewTime()).isEqualTo("14:00");
            assertThat(history.getNewDays()).isEqualTo("TUE,THU");
            assertThat(history.getNewEnabled()).isTrue();
            assertThat(history.getChangeReason()).isEqualTo("어르신 구두 요청");
            assertThat(history.getRelatedRequestId()).isNull();
        }

        @Test
        @DisplayName("이전 값이 null인 경우도 정상 동작")
        void createDirectUpdate_withNullPreviousValues() {
            // when
            CallScheduleHistory history = CallScheduleHistory.createDirectUpdate(
                    mockElderly,
                    mockCounselor,
                    null, null, null,
                    "14:00", "TUE,THU", true,
                    "최초 설정");

            // then
            assertThat(history.getPreviousTime()).isNull();
            assertThat(history.getPreviousDays()).isNull();
            assertThat(history.getPreviousEnabled()).isNull();
            assertThat(history.getNewTime()).isEqualTo("14:00");
        }
    }

    @Nested
    @DisplayName("createRequestApproved - 요청 승인 이력 생성")
    class CreateRequestApprovedTest {

        @Test
        @DisplayName("요청 승인 이력 생성 성공")
        void createRequestApproved_success() {
            // when
            CallScheduleHistory history = CallScheduleHistory.createRequestApproved(
                    mockElderly,
                    mockCounselor,
                    1L,
                    "09:00", "MON,WED,FRI", true,
                    "14:00", "TUE,THU");

            // then
            assertThat(history.getChangeType()).isEqualTo(CallScheduleHistory.ChangeType.REQUEST_APPROVED);
            assertThat(history.getRelatedRequestId()).isEqualTo(1L);
            assertThat(history.getNewEnabled()).isTrue(); // 승인 시 항상 true
            assertThat(history.getChangeReason()).isNull(); // 승인에는 사유 없음
        }

        @Test
        @DisplayName("승인 이력에 요청 ID 포함")
        void createRequestApproved_containsRequestId() {
            // when
            CallScheduleHistory history = CallScheduleHistory.createRequestApproved(
                    mockElderly,
                    mockCounselor,
                    999L,
                    "09:00", "MON", true,
                    "10:00", "TUE");

            // then
            assertThat(history.getRelatedRequestId()).isEqualTo(999L);
        }
    }

    @Nested
    @DisplayName("createRequestRejected - 요청 거절 이력 생성")
    class CreateRequestRejectedTest {

        @Test
        @DisplayName("요청 거절 이력 생성 성공")
        void createRequestRejected_success() {
            // when
            CallScheduleHistory history = CallScheduleHistory.createRequestRejected(
                    mockElderly,
                    mockCounselor,
                    1L,
                    "업무 시간 외 통화 요청");

            // then
            assertThat(history.getChangeType()).isEqualTo(CallScheduleHistory.ChangeType.REQUEST_REJECTED);
            assertThat(history.getRelatedRequestId()).isEqualTo(1L);
            assertThat(history.getChangeReason()).isEqualTo("업무 시간 외 통화 요청");
            // 거절은 스케줄 변경 없음
            assertThat(history.getPreviousTime()).isNull();
            assertThat(history.getNewTime()).isNull();
        }

        @Test
        @DisplayName("거절 사유 없이도 생성 가능")
        void createRequestRejected_withoutReason() {
            // when
            CallScheduleHistory history = CallScheduleHistory.createRequestRejected(
                    mockElderly,
                    mockCounselor,
                    1L,
                    null);

            // then
            assertThat(history.getChangeReason()).isNull();
            assertThat(history.getChangeType()).isEqualTo(CallScheduleHistory.ChangeType.REQUEST_REJECTED);
        }
    }

    @Nested
    @DisplayName("ChangeType Enum 테스트")
    class ChangeTypeTest {

        @Test
        @DisplayName("각 타입의 description 확인")
        void changeType_descriptions() {
            assertThat(CallScheduleHistory.ChangeType.DIRECT_UPDATE.getDescription())
                    .isEqualTo("직접 수정");
            assertThat(CallScheduleHistory.ChangeType.REQUEST_APPROVED.getDescription())
                    .isEqualTo("요청 승인");
            assertThat(CallScheduleHistory.ChangeType.REQUEST_REJECTED.getDescription())
                    .isEqualTo("요청 거절");
            assertThat(CallScheduleHistory.ChangeType.ELDERLY_SELF_UPDATE.getDescription())
                    .isEqualTo("본인 수정");
        }

        @Test
        @DisplayName("Enum values 확인")
        void changeType_values() {
            CallScheduleHistory.ChangeType[] types = CallScheduleHistory.ChangeType.values();
            assertThat(types).hasSize(4);
            assertThat(types).containsExactly(
                    CallScheduleHistory.ChangeType.DIRECT_UPDATE,
                    CallScheduleHistory.ChangeType.REQUEST_APPROVED,
                    CallScheduleHistory.ChangeType.REQUEST_REJECTED,
                    CallScheduleHistory.ChangeType.ELDERLY_SELF_UPDATE);
        }
    }
}
