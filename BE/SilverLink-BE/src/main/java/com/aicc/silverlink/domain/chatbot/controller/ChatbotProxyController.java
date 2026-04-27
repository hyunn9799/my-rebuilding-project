package com.aicc.silverlink.domain.chatbot.controller;

import com.aicc.silverlink.domain.chatbot.dto.ChatRequest;
import com.aicc.silverlink.domain.chatbot.dto.ChatResponse;
import com.aicc.silverlink.domain.chatbot.dto.ChatbotRequest;
import com.aicc.silverlink.domain.guardian.repository.GuardianElderlyRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * Python 챗봇 서비스와 프론트엔드 사이의 프록시 컨트롤러
 * 인증/권한 검증 후 Python 챗봇 서비스로 요청 전달
 */
@Slf4j
@RestController
@RequestMapping("/api/chatbot")
@RequiredArgsConstructor
@Tag(name = "Chatbot", description = "AI 챗봇 API")
public class ChatbotProxyController {

        private final RestTemplate restTemplate;
        private final GuardianElderlyRepository guardianElderlyRepository;

        @Value("${chatbot.python.url:http://localhost:5000}")
        private String pythonChatbotUrl;

        @Value("${chatbot.secret.header:X-SilverLink-Secret}")
        private String secretHeader;

        @Value("${chatbot.secret.key:X-SilverLink-Key!}")
        private String secretKey;

        @PostMapping("/chat")
        @Operation(summary = "챗봇 질문", description = "보호자가 어르신 돌봄 관련 질문을 하면 AI 챗봇이 FAQ 및 과거 문의 기록을 기반으로 답변합니다.")
        public ResponseEntity<ChatResponse> chat(
                        @RequestBody ChatRequest request,
                        @AuthenticationPrincipal Long authenticatedGuardianId) {
                log.info("Chatbot request received: guardianId={}, elderlyId={}, message={}",
                                request.getGuardianId(), request.getElderlyId(), request.getMessage());

                // 2. 권한 검증: 요청한 guardianId와 인증된 guardianId 일치 확인
                if (!authenticatedGuardianId.equals(request.getGuardianId())) {
                        log.warn("Unauthorized access attempt: authenticated={}, requested={}",
                                        authenticatedGuardianId, request.getGuardianId());
                        throw new SecurityException("권한이 없습니다");
                }

                // 3. 보호자-어르신 관계 검증 (추가 보안)
                boolean isValidRelation = guardianElderlyRepository
                                .existsByGuardianIdAndElderlyId(
                                                request.getGuardianId(),
                                                request.getElderlyId());

                if (!isValidRelation) {
                        log.warn("Invalid guardian-elderly relation: guardianId={}, elderlyId={}",
                                        request.getGuardianId(), request.getElderlyId());
                        throw new SecurityException("잘못된 어르신 정보입니다");
                }

                // 4. Thread ID 생성 (클라이언트 제공 값 우선, 없으면 보호자 ID 기반)
                String threadId = request.getThreadId();
                if (threadId == null || threadId.isEmpty()) {
                        threadId = "guardian_" + request.getGuardianId();
                }

                // 5. Python 챗봇 서비스 요청 생성
                ChatbotRequest chatbotRequest = ChatbotRequest.builder()
                                .message(request.getMessage())
                                .threadId(threadId)
                                .guardianId(request.getGuardianId())
                                .elderlyId(request.getElderlyId())
                                .build();

                // 6. Python 챗봇 서비스 호출
                log.info("Calling Python chatbot service: url={}, threadId={}",
                                pythonChatbotUrl, threadId);

                try {
                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.APPLICATION_JSON);
                        headers.set(secretHeader, secretKey);

                        HttpEntity<ChatbotRequest> entity = new HttpEntity<>(chatbotRequest, headers);

                        ResponseEntity<ChatResponse> responseEntity = restTemplate.exchange(
                                pythonChatbotUrl + "/api/chatbot/chat",
                                HttpMethod.POST,
                                entity,
                                ChatResponse.class
                        );

                        ChatResponse response = responseEntity.getBody();

                        log.info("Chatbot response received: threadId={}, confidence={}",
                                        threadId, response != null ? response.getConfidence() : null);

                        return ResponseEntity.ok(response);

                } catch (Exception e) {
                        log.error("Error calling Python chatbot service", e);
                        throw new RuntimeException("챗봇 서비스 호출 중 오류가 발생했습니다", e);
                }
        }
}
