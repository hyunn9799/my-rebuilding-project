package com.aicc.silverlink.domain.elderly.dto;

import com.aicc.silverlink.domain.elderly.dto.ScheduleChangeRequestDto.*;
import com.aicc.silverlink.domain.elderly.entity.Elderly;
import com.aicc.silverlink.domain.elderly.entity.ScheduleChangeRequest;
import com.aicc.silverlink.domain.elderly.entity.ScheduleChangeRequest.RequestStatus;
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * ScheduleChangeRequestDto 단위 테스트
 */
@DisplayName("ScheduleChangeRequestDto 테스트")
class ScheduleChangeRequestDtoTest {

    @Nested
    @DisplayName("CreateRequest 테스트")
    class CreateRequestTest {

        @Test
        @DisplayName("getDaysAsString - 요일 리스트를 콤마 구분 문자열로 변환")
        void getDaysAsString_convertsList() {
            // given
            CreateRequest request = new CreateRequest();
            request.setPreferredCallTime("14:00");
            request.setPreferredCallDays(List.of("MON", "WED", "FRI"));

            // when
            String result = request.getDaysAsString();

            // then
            assertThat(result).isEqualTo("MON,WED,FRI");
        }

        @Test
        @DisplayName("getDaysAsString - 단일 요일")
        void getDaysAsString_singleDay() {
            // given
            CreateRequest request = new CreateRequest();
            request.setPreferredCallDays(List.of("MON"));

            // when
            String result = request.getDaysAsString();

            // then
            assertThat(result).isEqualTo("MON");
        }

        @Test
        @DisplayName("getDaysAsString - 빈 리스트면 null 반환")
        void getDaysAsString_emptyList_returnsNull() {
            // given
            CreateRequest request = new CreateRequest();
            request.setPreferredCallDays(List.of());

            // when
            String result = request.getDaysAsString();

            // then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("getDaysAsString - null이면 null 반환")
        void getDaysAsString_null_returnsNull() {
            // given
            CreateRequest request = new CreateRequest();
            request.setPreferredCallDays(null);

            // when
            String result = request.getDaysAsString();

            // then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("RejectRequest 테스트")
    class RejectRequestTest {

        @Test
        @DisplayName("기본 생성자로 생성 후 사유 설정")
        void defaultConstructor_thenSetReason() {
            // given
            RejectRequest request = new RejectRequest();

            // when
            request.setReason("업무 시간 외 통화 요청");

            // then
            assertThat(request.getReason()).isEqualTo("업무 시간 외 통화 요청");
        }

        @Test
        @DisplayName("AllArgsConstructor로 생성")
        void allArgsConstructor() {
            // when
            RejectRequest request = new RejectRequest("사유입니다");

            // then
            assertThat(request.getReason()).isEqualTo("사유입니다");
        }
    }

    @Nested
    @DisplayName("Response 테스트")
    class ResponseTest {

        @Test
        @DisplayName("from 메서드 - PENDING 상태 변환")
        void from_pendingStatus() {
            // given
            User elderlyUser = mock(User.class);
            when(elderlyUser.getName()).thenReturn("홍길동");

            Elderly elderly = mock(Elderly.class);
            when(elderly.getId()).thenReturn(1L);
            when(elderly.getUser()).thenReturn(elderlyUser);

            ScheduleChangeRequest request = mock(ScheduleChangeRequest.class);
            when(request.getId()).thenReturn(1L);
            when(request.getElderly()).thenReturn(elderly);
            when(request.getRequestedCallTime()).thenReturn("14:00");
            when(request.getRequestedCallDays()).thenReturn("TUE,THU");
            when(request.getStatus()).thenReturn(RequestStatus.PENDING);
            when(request.getCreatedAt()).thenReturn(LocalDateTime.of(2025, 1, 29, 10, 0));
            when(request.getProcessedAt()).thenReturn(null);
            when(request.getProcessedBy()).thenReturn(null);
            when(request.getRejectReason()).thenReturn(null);

            // when
            Response response = Response.from(request);

            // then
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getElderlyId()).isEqualTo(1L);
            assertThat(response.getElderlyName()).isEqualTo("홍길동");
            assertThat(response.getRequestedCallTime()).isEqualTo("14:00");
            assertThat(response.getRequestedCallDays()).containsExactly("TUE", "THU");
            assertThat(response.getStatus()).isEqualTo(RequestStatus.PENDING);
            assertThat(response.getProcessedAt()).isNull();
            assertThat(response.getProcessedByName()).isNull();
            assertThat(response.getRejectReason()).isNull();
        }

        @Test
        @DisplayName("from 메서드 - APPROVED 상태 변환")
        void from_approvedStatus() {
            // given
            User elderlyUser = mock(User.class);
            when(elderlyUser.getName()).thenReturn("홍길동");

            User counselor = mock(User.class);
            when(counselor.getName()).thenReturn("박상담");

            Elderly elderly = mock(Elderly.class);
            when(elderly.getId()).thenReturn(1L);
            when(elderly.getUser()).thenReturn(elderlyUser);

            ScheduleChangeRequest request = mock(ScheduleChangeRequest.class);
            when(request.getId()).thenReturn(1L);
            when(request.getElderly()).thenReturn(elderly);
            when(request.getRequestedCallTime()).thenReturn("14:00");
            when(request.getRequestedCallDays()).thenReturn("TUE,THU");
            when(request.getStatus()).thenReturn(RequestStatus.APPROVED);
            when(request.getCreatedAt()).thenReturn(LocalDateTime.of(2025, 1, 29, 10, 0));
            when(request.getProcessedAt()).thenReturn(LocalDateTime.of(2025, 1, 29, 11, 0));
            when(request.getProcessedBy()).thenReturn(counselor);
            when(request.getRejectReason()).thenReturn(null);

            // when
            Response response = Response.from(request);

            // then
            assertThat(response.getStatus()).isEqualTo(RequestStatus.APPROVED);
            assertThat(response.getProcessedAt()).isNotNull();
            assertThat(response.getProcessedByName()).isEqualTo("박상담");
            assertThat(response.getRejectReason()).isNull();
        }

        @Test
        @DisplayName("from 메서드 - REJECTED 상태 변환")
        void from_rejectedStatus() {
            // given
            User elderlyUser = mock(User.class);
            when(elderlyUser.getName()).thenReturn("홍길동");

            User counselor = mock(User.class);
            when(counselor.getName()).thenReturn("박상담");

            Elderly elderly = mock(Elderly.class);
            when(elderly.getId()).thenReturn(1L);
            when(elderly.getUser()).thenReturn(elderlyUser);

            ScheduleChangeRequest request = mock(ScheduleChangeRequest.class);
            when(request.getId()).thenReturn(1L);
            when(request.getElderly()).thenReturn(elderly);
            when(request.getRequestedCallTime()).thenReturn("14:00");
            when(request.getRequestedCallDays()).thenReturn("TUE,THU");
            when(request.getStatus()).thenReturn(RequestStatus.REJECTED);
            when(request.getCreatedAt()).thenReturn(LocalDateTime.of(2025, 1, 29, 10, 0));
            when(request.getProcessedAt()).thenReturn(LocalDateTime.of(2025, 1, 29, 11, 0));
            when(request.getProcessedBy()).thenReturn(counselor);
            when(request.getRejectReason()).thenReturn("업무 시간 외 통화 요청");

            // when
            Response response = Response.from(request);

            // then
            assertThat(response.getStatus()).isEqualTo(RequestStatus.REJECTED);
            assertThat(response.getRejectReason()).isEqualTo("업무 시간 외 통화 요청");
        }

        @Test
        @DisplayName("from 메서드 - 요일 파싱 (빈 문자열)")
        void from_emptyDays() {
            // given
            User elderlyUser = mock(User.class);
            when(elderlyUser.getName()).thenReturn("홍길동");

            Elderly elderly = mock(Elderly.class);
            when(elderly.getId()).thenReturn(1L);
            when(elderly.getUser()).thenReturn(elderlyUser);

            ScheduleChangeRequest request = mock(ScheduleChangeRequest.class);
            when(request.getId()).thenReturn(1L);
            when(request.getElderly()).thenReturn(elderly);
            when(request.getRequestedCallTime()).thenReturn("14:00");
            when(request.getRequestedCallDays()).thenReturn("");
            when(request.getStatus()).thenReturn(RequestStatus.PENDING);
            when(request.getCreatedAt()).thenReturn(LocalDateTime.now());

            // when
            Response response = Response.from(request);

            // then
            assertThat(response.getRequestedCallDays()).isEmpty();
        }
    }
}
