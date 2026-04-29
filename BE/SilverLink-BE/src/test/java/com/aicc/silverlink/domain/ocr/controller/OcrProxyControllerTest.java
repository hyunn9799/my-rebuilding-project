package com.aicc.silverlink.domain.ocr.controller;

import com.aicc.silverlink.domain.assignment.repository.AssignmentRepository;
import com.aicc.silverlink.domain.guardian.repository.GuardianElderlyRepository;
import com.aicc.silverlink.domain.ocr.dto.ConfirmMedicationRequest;
import com.aicc.silverlink.domain.ocr.dto.ConfirmMedicationResponse;
import com.aicc.silverlink.domain.ocr.dto.OcrResultOwnerResponse;
import com.aicc.silverlink.domain.ocr.dto.OcrValidationRequest;
import com.aicc.silverlink.domain.ocr.dto.OcrValidationResponse;
import com.aicc.silverlink.domain.ocr.dto.PythonOcrRequest;
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({ "unchecked", "rawtypes" })
class OcrProxyControllerTest {

    private static final String PYTHON_URL = "http://localhost:8000";

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GuardianElderlyRepository guardianElderlyRepository;

    @Mock
    private AssignmentRepository assignmentRepository;

    private OcrProxyController controller;

    @BeforeEach
    void setUp() {
        controller = new OcrProxyController(restTemplate, userRepository, guardianElderlyRepository, assignmentRepository);
        ReflectionTestUtils.setField(controller, "pythonAiUrl", PYTHON_URL);
        ReflectionTestUtils.setField(controller, "secretHeader", "X-SilverLink-Secret");
        ReflectionTestUtils.setField(controller, "secretKey", "test-secret");
        authenticate(10L, Role.ELDERLY);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("elderly user can validate OCR for self and missing elderlyUserId defaults to principal")
    void validateMedicationAllowsElderlySelfAndDefaultsElderlyUserId() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(User.createFake(10L, Role.ELDERLY)));
        when(restTemplate.exchange(
                eq(PYTHON_URL + "/api/ocr/validate-medication"),
                eq(HttpMethod.POST),
                org.mockito.ArgumentMatchers.<HttpEntity<PythonOcrRequest>>any(),
                eq(OcrValidationResponse.class)))
                .thenReturn(ResponseEntity.ok(OcrValidationResponse.builder().success(true).build()));

        ArgumentCaptor<HttpEntity<PythonOcrRequest>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

        ResponseEntity<OcrValidationResponse> response = controller.validateMedication(
                OcrValidationRequest.builder()
                        .ocrText("타이레놀")
                        .build());

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getSuccess()).isTrue();
        verify(restTemplate).exchange(
                eq(PYTHON_URL + "/api/ocr/validate-medication"),
                eq(HttpMethod.POST),
                entityCaptor.capture(),
                eq(OcrValidationResponse.class));
        assertThat(entityCaptor.getValue().getBody().getElderlyUserId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("guardian without relation cannot query pending OCR confirmations")
    void pendingConfirmationsRejectsUnrelatedGuardian() {
        authenticate(20L, Role.GUARDIAN);
        when(userRepository.findById(20L)).thenReturn(Optional.of(User.createFake(20L, Role.GUARDIAN)));
        when(guardianElderlyRepository.existsByGuardianIdAndElderlyId(20L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> controller.getPendingConfirmations(10L))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");

        verify(restTemplate, never()).exchange(
                eq(PYTHON_URL + "/api/ocr/pending-confirmations/10"),
                eq(HttpMethod.GET),
                org.mockito.ArgumentMatchers.<HttpEntity<Void>>any(),
                eq(com.aicc.silverlink.domain.ocr.dto.PendingConfirmationItem[].class));
    }

    @Test
    @DisplayName("counselor with active assignment can query pending OCR confirmations")
    void pendingConfirmationsAllowsAssignedCounselor() {
        authenticate(30L, Role.COUNSELOR);
        when(userRepository.findById(30L)).thenReturn(Optional.of(User.createFake(30L, Role.COUNSELOR)));
        when(assignmentRepository.existsByCounselorIdAndElderlyIdAndStatusActive(30L, 10L)).thenReturn(true);
        when(restTemplate.exchange(
                eq(PYTHON_URL + "/api/ocr/pending-confirmations/10"),
                eq(HttpMethod.GET),
                org.mockito.ArgumentMatchers.<HttpEntity<Void>>any(),
                eq(com.aicc.silverlink.domain.ocr.dto.PendingConfirmationItem[].class)))
                .thenReturn(ResponseEntity.ok(new com.aicc.silverlink.domain.ocr.dto.PendingConfirmationItem[0]));

        ResponseEntity<?> response = controller.getPendingConfirmations(10L);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    }

    @Test
    @DisplayName("guardian without relation cannot confirm another elderly user's OCR request")
    void confirmMedicationRejectsUnrelatedGuardian() {
        authenticate(20L, Role.GUARDIAN);
        when(restTemplate.exchange(
                eq(PYTHON_URL + "/api/ocr/internal/results/req-1/owner"),
                eq(HttpMethod.GET),
                org.mockito.ArgumentMatchers.<HttpEntity<Void>>any(),
                eq(OcrResultOwnerResponse.class)))
                .thenReturn(ResponseEntity.ok(ownerResponse(10L, null)));
        when(userRepository.findById(20L)).thenReturn(Optional.of(User.createFake(20L, Role.GUARDIAN)));
        when(guardianElderlyRepository.existsByGuardianIdAndElderlyId(20L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> controller.confirmMedication(confirmRequest("req-1")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403 FORBIDDEN");

        verify(restTemplate, never()).exchange(
                eq(PYTHON_URL + "/api/ocr/confirm-medication"),
                eq(HttpMethod.POST),
                org.mockito.ArgumentMatchers.<HttpEntity<ConfirmMedicationRequest>>any(),
                eq(ConfirmMedicationResponse.class));
    }

    @Test
    @DisplayName("assigned counselor can confirm OCR request after owner lookup")
    void confirmMedicationAllowsAssignedCounselor() {
        authenticate(30L, Role.COUNSELOR);
        when(restTemplate.exchange(
                eq(PYTHON_URL + "/api/ocr/internal/results/req-1/owner"),
                eq(HttpMethod.GET),
                org.mockito.ArgumentMatchers.<HttpEntity<Void>>any(),
                eq(OcrResultOwnerResponse.class)))
                .thenReturn(ResponseEntity.ok(ownerResponse(10L, null)));
        when(userRepository.findById(30L)).thenReturn(Optional.of(User.createFake(30L, Role.COUNSELOR)));
        when(assignmentRepository.existsByCounselorIdAndElderlyIdAndStatusActive(30L, 10L)).thenReturn(true);
        when(restTemplate.exchange(
                eq(PYTHON_URL + "/api/ocr/confirm-medication"),
                eq(HttpMethod.POST),
                org.mockito.ArgumentMatchers.<HttpEntity<ConfirmMedicationRequest>>any(),
                eq(ConfirmMedicationResponse.class)))
                .thenReturn(ResponseEntity.ok(ConfirmMedicationResponse.builder()
                        .success(true)
                        .message("Confirmation processed.")
                        .build()));

        ResponseEntity<ConfirmMedicationResponse> response = controller.confirmMedication(confirmRequest("req-1"));

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getSuccess()).isTrue();
    }

    @Test
    @DisplayName("owner lookup conflict prevents confirm API call")
    void confirmMedicationStopsWhenOwnerLookupConflict() {
        when(restTemplate.exchange(
                eq(PYTHON_URL + "/api/ocr/internal/results/req-1/owner"),
                eq(HttpMethod.GET),
                org.mockito.ArgumentMatchers.<HttpEntity<Void>>any(),
                eq(OcrResultOwnerResponse.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.CONFLICT, "already confirmed"));

        assertThatThrownBy(() -> controller.confirmMedication(confirmRequest("req-1")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");

        verify(restTemplate, never()).exchange(
                eq(PYTHON_URL + "/api/ocr/confirm-medication"),
                eq(HttpMethod.POST),
                org.mockito.ArgumentMatchers.<HttpEntity<ConfirmMedicationRequest>>any(),
                eq(ConfirmMedicationResponse.class));
    }

    @Test
    @DisplayName("confirm API 4xx response is preserved after owner authorization succeeds")
    void confirmMedicationPreservesConfirmApiClientError() {
        when(restTemplate.exchange(
                eq(PYTHON_URL + "/api/ocr/internal/results/req-1/owner"),
                eq(HttpMethod.GET),
                org.mockito.ArgumentMatchers.<HttpEntity<Void>>any(),
                eq(OcrResultOwnerResponse.class)))
                .thenReturn(ResponseEntity.ok(ownerResponse(10L, null)));
        when(userRepository.findById(10L)).thenReturn(Optional.of(User.createFake(10L, Role.ELDERLY)));
        when(restTemplate.exchange(
                eq(PYTHON_URL + "/api/ocr/confirm-medication"),
                eq(HttpMethod.POST),
                org.mockito.ArgumentMatchers.<HttpEntity<ConfirmMedicationRequest>>any(),
                eq(ConfirmMedicationResponse.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.UNPROCESSABLE_ENTITY, "invalid candidate"));

        ResponseEntity<ConfirmMedicationResponse> response = controller.confirmMedication(confirmRequest("req-1"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getSuccess()).isFalse();
    }

    @Test
    @DisplayName("pending confirmation API 4xx response is preserved")
    void pendingConfirmationsPreservesAiClientError() {
        when(userRepository.findById(10L)).thenReturn(Optional.of(User.createFake(10L, Role.ELDERLY)));
        when(restTemplate.exchange(
                eq(PYTHON_URL + "/api/ocr/pending-confirmations/10"),
                eq(HttpMethod.GET),
                org.mockito.ArgumentMatchers.<HttpEntity<Void>>any(),
                eq(com.aicc.silverlink.domain.ocr.dto.PendingConfirmationItem[].class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND, "missing"));

        ResponseEntity<?> response = controller.getPendingConfirmations(10L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private void authenticate(Long userId, Role role) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userId,
                null,
                List.of(new SimpleGrantedAuthority(role.asAuthority())));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private ConfirmMedicationRequest confirmRequest(String requestId) {
        return ConfirmMedicationRequest.builder()
                .requestId(requestId)
                .selectedItemSeq("ITEM-1")
                .confirmed(true)
                .build();
    }

    private OcrResultOwnerResponse ownerResponse(Long elderlyUserId, Boolean userConfirmed) {
        return OcrResultOwnerResponse.builder()
                .requestId("req-1")
                .elderlyUserId(elderlyUserId)
                .decisionStatus("NEED_USER_CONFIRMATION")
                .userConfirmed(userConfirmed)
                .build();
    }
}
