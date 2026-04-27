package com.aicc.silverlink.domain.elderly.service;

import com.aicc.silverlink.domain.elderly.dto.CallScheduleDto.*;
import com.aicc.silverlink.domain.elderly.entity.CallScheduleHistory;
import com.aicc.silverlink.domain.elderly.entity.Elderly;
import com.aicc.silverlink.domain.elderly.repository.CallScheduleHistoryRepository;
import com.aicc.silverlink.domain.elderly.repository.ElderlyRepository;
import com.aicc.silverlink.domain.elderly.repository.HealthInfoRepository;
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CallScheduleService 테스트")
class CallScheduleServiceTest {

    @Mock
    private ElderlyRepository elderlyRepository;

    @Mock
    private HealthInfoRepository healthInfoRepository;

    @Mock
    private CallScheduleHistoryRepository historyRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CallScheduleService callScheduleService;

    private User mockUser;
    private User mockCounselor;
    private Elderly mockElderly;

    @BeforeEach
    void setUp() {
        mockUser = mock(User.class);
        lenient().when(mockUser.getId()).thenReturn(1L);
        lenient().when(mockUser.getName()).thenReturn("홍길동");

        mockCounselor = mock(User.class);
        lenient().when(mockCounselor.getId()).thenReturn(100L);
        lenient().when(mockCounselor.getName()).thenReturn("상담사");
        lenient().when(mockCounselor.getRole()).thenReturn(Role.COUNSELOR);

        mockElderly = mock(Elderly.class);
        lenient().when(mockElderly.getId()).thenReturn(1L);
        lenient().when(mockElderly.getUser()).thenReturn(mockUser);
        lenient().when(mockElderly.getPreferredCallTime()).thenReturn("09:00");
        lenient().when(mockElderly.getPreferredCallDays()).thenReturn("MON,WED,FRI");
        lenient().when(mockElderly.getCallScheduleEnabled()).thenReturn(true);
    }

    @Nested
    @DisplayName("getSchedule - 스케줄 조회")
    class GetScheduleTest {

        @Test
        @DisplayName("어르신 ID로 스케줄 조회 성공")
        void getSchedule_success() {
            // given
            given(elderlyRepository.findWithUserById(1L)).willReturn(Optional.of(mockElderly));

            // when
            Response response = callScheduleService.getSchedule(1L);

            // then
            assertThat(response.getElderlyId()).isEqualTo(1L);
            assertThat(response.getElderlyName()).isEqualTo("홍길동");
            assertThat(response.getPreferredCallTime()).isEqualTo("09:00");
            assertThat(response.getPreferredCallDays()).containsExactly("MON", "WED", "FRI");
            assertThat(response.getCallScheduleEnabled()).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 어르신 조회 시 예외 발생")
        void getSchedule_notFound() {
            // given
            given(elderlyRepository.findWithUserById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> callScheduleService.getSchedule(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("어르신을 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("updateSchedule - 스케줄 수정")
    class UpdateScheduleTest {

        @Test
        @DisplayName("스케줄 수정 성공")
        void updateSchedule_success() {
            // given
            given(elderlyRepository.findWithUserById(1L)).willReturn(Optional.of(mockElderly));

            UpdateRequest request = new UpdateRequest();
            request.setPreferredCallTime("10:00");
            request.setPreferredCallDays(List.of("TUE", "THU"));
            request.setCallScheduleEnabled(true);

            // when
            Response response = callScheduleService.updateSchedule(1L, request);

            // then
            verify(mockElderly).updateCallSchedule("10:00", "TUE,THU", true);
            verify(elderlyRepository).save(mockElderly);
        }
    }

    @Nested
    @DisplayName("directUpdateSchedule - 직접 수정")
    class DirectUpdateScheduleTest {

        @Test
        @DisplayName("상담사가 직접 스케줄 수정 시 이력 저장됨")
        void directUpdate_savesHistory() {
            // given
            given(elderlyRepository.findWithUserById(1L)).willReturn(Optional.of(mockElderly));
            given(userRepository.findById(100L)).willReturn(Optional.of(mockCounselor));

            DirectUpdateRequest request = new DirectUpdateRequest();
            request.setPreferredCallTime("14:00");
            request.setPreferredCallDays(List.of("MON", "FRI"));
            request.setCallScheduleEnabled(true);
            request.setChangeReason("어르신 구두 요청");

            // when
            callScheduleService.directUpdateSchedule(1L, 100L, request);

            // then
            verify(mockElderly).updateCallSchedule("14:00", "MON,FRI", true);
            verify(elderlyRepository).save(mockElderly);

            ArgumentCaptor<CallScheduleHistory> historyCaptor = ArgumentCaptor.forClass(CallScheduleHistory.class);
            verify(historyRepository).save(historyCaptor.capture());

            CallScheduleHistory savedHistory = historyCaptor.getValue();
            assertThat(savedHistory.getChangeType()).isEqualTo(CallScheduleHistory.ChangeType.DIRECT_UPDATE);
            assertThat(savedHistory.getChangeReason()).isEqualTo("어르신 구두 요청");
        }

        @Test
        @DisplayName("존재하지 않는 사용자로 수정 시 예외 발생")
        void directUpdate_userNotFound() {
            // given
            given(elderlyRepository.findWithUserById(1L)).willReturn(Optional.of(mockElderly));
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            DirectUpdateRequest request = new DirectUpdateRequest();

            // when & then
            assertThatThrownBy(() -> callScheduleService.directUpdateSchedule(1L, 999L, request))
                    .hasMessageContaining("사용자를 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("getHistoryByElderly - 어르신별 이력 조회")
    class GetHistoryByElderlyTest {

        @Test
        @DisplayName("어르신별 이력 조회 성공")
        void getHistory_success() {
            // given
            CallScheduleHistory mockHistory = mock(CallScheduleHistory.class);
            when(mockHistory.getId()).thenReturn(1L);
            when(mockHistory.getElderly()).thenReturn(mockElderly);
            when(mockHistory.getChangedBy()).thenReturn(mockCounselor);
            when(mockHistory.getChangeType()).thenReturn(CallScheduleHistory.ChangeType.DIRECT_UPDATE);
            when(mockHistory.getPreviousTime()).thenReturn("09:00");
            when(mockHistory.getNewTime()).thenReturn("14:00");

            given(historyRepository.findByElderlyIdOrderByCreatedAtDesc(1L))
                    .willReturn(List.of(mockHistory));

            // when
            List<HistoryResponse> result = callScheduleService.getHistoryByElderly(1L);

            // then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getChangeType()).isEqualTo("DIRECT_UPDATE");
            assertThat(result.get(0).getPreviousTime()).isEqualTo("09:00");
            assertThat(result.get(0).getNewTime()).isEqualTo("14:00");
        }
    }

    @Nested
    @DisplayName("getAllHistory - 전체 이력 조회")
    class GetAllHistoryTest {

        @Test
        @DisplayName("관리자용 전체 이력 조회")
        void getAllHistory_success() {
            // given
            given(historyRepository.findAllWithDetails()).willReturn(List.of());

            // when
            List<HistoryResponse> result = callScheduleService.getAllHistory();

            // then
            verify(historyRepository).findAllWithDetails();
            assertThat(result).isEmpty();
        }
    }
}
