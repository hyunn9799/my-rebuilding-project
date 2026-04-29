package com.aicc.silverlink.domain.ocr.controller;

import com.aicc.silverlink.domain.ocr.dto.AliasSuggestionActionResponse;
import com.aicc.silverlink.domain.ocr.dto.AliasSuggestionPageResponse;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({ "unchecked", "rawtypes" })
class AdminAliasControllerTest {

    private static final String PYTHON_URL = "http://localhost:8000";
    private static final String SECRET_HEADER = "X-SilverLink-Secret";
    private static final String SECRET_KEY = "test-secret";

    @Mock
    private RestTemplate restTemplate;

    private AdminAliasController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminAliasController(restTemplate);
        ReflectionTestUtils.setField(controller, "pythonAiUrl", PYTHON_URL);
        ReflectionTestUtils.setField(controller, "secretHeader", SECRET_HEADER);
        ReflectionTestUtils.setField(controller, "secretKey", SECRET_KEY);
    }

    @Test
    @DisplayName("alias suggestion list request is proxied to Python admin endpoint")
    void getAliasSuggestionsProxiesToPythonEndpoint() {
        AliasSuggestionPageResponse body = AliasSuggestionPageResponse.builder()
                .items(List.of(AliasSuggestionPageResponse.AliasSuggestionItem.builder()
                        .id(10L)
                        .aliasName("codex_alias")
                        .reviewStatus("PENDING")
                        .build()))
                .total(1)
                .page(2)
                .size(5)
                .build();

        ArgumentCaptor<HttpEntity<Void>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        when(restTemplate.exchange(
                eq(PYTHON_URL + "/api/ocr/admin/alias-suggestions?page=2&size=5&review_status=PENDING"),
                eq(HttpMethod.GET),
                entityCaptor.capture(),
                eq(AliasSuggestionPageResponse.class)))
                .thenReturn(ResponseEntity.ok(body));

        ResponseEntity<AliasSuggestionPageResponse> response = controller.getAliasSuggestions(2, 5, "PENDING");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(body);
        assertThat(entityCaptor.getValue().getHeaders().getFirst(SECRET_HEADER)).isEqualTo(SECRET_KEY);
    }

    @Test
    @DisplayName("approve request is proxied to Python approve endpoint")
    void approveSuggestionProxiesToPythonEndpoint() {
        AliasSuggestionActionResponse body = AliasSuggestionActionResponse.builder()
                .success(true)
                .message("approved")
                .targetTable("medication_aliases")
                .build();

        ArgumentCaptor<HttpEntity<Void>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        when(restTemplate.exchange(
                eq(PYTHON_URL + "/api/ocr/admin/alias-suggestions/10/approve?reviewed_by=admin-user"),
                eq(HttpMethod.PUT),
                entityCaptor.capture(),
                eq(AliasSuggestionActionResponse.class)))
                .thenReturn(ResponseEntity.ok(body));

        ResponseEntity<AliasSuggestionActionResponse> response = controller.approveSuggestion(10L, "admin-user");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(body);
        assertThat(entityCaptor.getValue().getHeaders().getFirst(SECRET_HEADER)).isEqualTo(SECRET_KEY);
    }

    @Test
    @DisplayName("reject request is proxied to Python reject endpoint")
    void rejectSuggestionProxiesToPythonEndpoint() {
        AliasSuggestionActionResponse body = AliasSuggestionActionResponse.builder()
                .success(true)
                .message("rejected")
                .build();

        when(restTemplate.exchange(
                eq(PYTHON_URL + "/api/ocr/admin/alias-suggestions/11/reject?reviewed_by=admin-user"),
                eq(HttpMethod.PUT),
                org.mockito.ArgumentMatchers.<HttpEntity<Void>>any(),
                eq(AliasSuggestionActionResponse.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.ACCEPTED).body(body));

        ResponseEntity<AliasSuggestionActionResponse> response = controller.rejectSuggestion(11L, "admin-user");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isSameAs(body);
    }

    @Test
    @DisplayName("reload request is proxied to Python reload endpoint")
    void reloadDictionaryProxiesToPythonEndpoint() {
        AliasSuggestionActionResponse body = AliasSuggestionActionResponse.builder()
                .success(true)
                .message("reloaded")
                .build();

        when(restTemplate.exchange(
                eq(PYTHON_URL + "/api/ocr/admin/reload-dictionary"),
                eq(HttpMethod.POST),
                org.mockito.ArgumentMatchers.<HttpEntity<Void>>any(),
                eq(AliasSuggestionActionResponse.class)))
                .thenReturn(ResponseEntity.ok(body));

        ResponseEntity<AliasSuggestionActionResponse> response = controller.reloadDictionary();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(body);

        verify(restTemplate).exchange(
                eq(PYTHON_URL + "/api/ocr/admin/reload-dictionary"),
                eq(HttpMethod.POST),
                org.mockito.ArgumentMatchers.<HttpEntity<Void>>any(),
                eq(AliasSuggestionActionResponse.class));
    }
}
