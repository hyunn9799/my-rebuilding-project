package com.aicc.silverlink.domain.medication.service;

import com.aicc.silverlink.domain.elderly.entity.Elderly;
import com.aicc.silverlink.domain.elderly.repository.ElderlyRepository;
import com.aicc.silverlink.domain.medication.dto.MedicationRequest;
import com.aicc.silverlink.domain.medication.dto.MedicationResponse;
import com.aicc.silverlink.domain.medication.entity.MedicationSchedule;
import com.aicc.silverlink.domain.medication.repository.MedicationScheduleRepository;
import com.aicc.silverlink.domain.medication.repository.MedicationScheduleTimeRepository;
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MedicationServiceTest {

    @Mock
    private MedicationScheduleRepository scheduleRepository;

    @Mock
    private MedicationScheduleTimeRepository scheduleTimeRepository;

    @Mock
    private ElderlyRepository elderlyRepository;

    @Mock
    private UserRepository userRepository;

    private MedicationService service;

    @BeforeEach
    void setUp() {
        service = new MedicationService(
                scheduleRepository,
                scheduleTimeRepository,
                elderlyRepository,
                userRepository);
    }

    @Test
    @DisplayName("OCR request id is stored when creating medication from OCR flow")
    void createMedicationStoresSourceOcrRequestId() {
        User user = User.createFake(10L, Role.ELDERLY);
        Elderly elderly = Elderly.builder()
                .id(10L)
                .user(user)
                .birthDate(LocalDate.of(1940, 1, 1))
                .gender(Elderly.Gender.F)
                .build();
        MedicationRequest request = new MedicationRequest(
                "타이레놀정500밀리그람",
                "500mg",
                List.of("morning", "evening"),
                "식후 복용",
                null,
                null,
                true,
                " req-ocr-1 ");

        when(elderlyRepository.findById(10L)).thenReturn(Optional.of(elderly));
        when(userRepository.findById(10L)).thenReturn(Optional.of(user));
        when(scheduleRepository.save(any(MedicationSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MedicationResponse response = service.createMedication(10L, request);

        ArgumentCaptor<MedicationSchedule> scheduleCaptor = ArgumentCaptor.forClass(MedicationSchedule.class);
        verify(scheduleRepository).save(scheduleCaptor.capture());
        assertThat(scheduleCaptor.getValue().getSourceOcrRequestId()).isEqualTo("req-ocr-1");
        assertThat(response.getSourceOcrRequestId()).isEqualTo("req-ocr-1");
    }
}
