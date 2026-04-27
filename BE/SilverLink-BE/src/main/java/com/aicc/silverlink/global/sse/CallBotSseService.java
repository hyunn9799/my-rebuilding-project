package com.aicc.silverlink.global.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class CallBotSseService {

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter connect(Long callId) {
        log.info("🔌 [SSE] 연결 시도: callId={}", callId);

        // 기존 연결이 있다면 제거 (Optional: 중복 연결 허용 정책에 따라 다름)
        // 현재는 1:1 매핑이므로 기존 연결을 덮어씀.
        // 다만, 기존 연결의 콜백이 나중에 실행되어 새 연결을 지우지 않도록 하는 것이 핵심.

        SseEmitter emitter = new SseEmitter(60 * 1000L * 30); // 30분 타임아웃
        emitters.put(callId, emitter);

        emitter.onCompletion(() -> {
            log.info("✅ [SSE] 연결 정상 종료: callId={}", callId);
            // 핵심 수정: 현재 맵에 있는 객체가 '이 emitter'일 때만 제거
            emitters.remove(callId, emitter);
        });

        emitter.onTimeout(() -> {
            log.warn("⏰ [SSE] 타임아웃 발생: callId={}", callId);
            emitters.remove(callId, emitter);
        });

        emitter.onError(e -> {
            log.error("❌ [SSE] 에러 발생: callId={}, error={}", callId, e.getMessage());
            emitters.remove(callId, emitter);
        });

        // 연결 확인용 더미 이벤트 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("connected"));
            log.info("✅ [SSE] 연결 성공: callId={}, 현재 활성 연결 수={}", callId, emitters.size());
        } catch (IOException e) {
            log.error("❌ [SSE] 초기 연결 실패: callId={}, error={}", callId, e.getMessage(), e);
            emitters.remove(callId, emitter);
        }

        return emitter;
    }

    public void broadcast(Long callId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(callId);
        if (emitter == null) {
            log.warn("⚠️ [SSE Broadcast] 활성 연결 없음: callId={}, event={}", callId, eventName);
            return;
        }

        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
            log.info("📤 [SSE Broadcast] 전송 성공: callId={}, event={}, dataLength={}",
                    callId, eventName, data != null ? data.toString().length() : 0);
        } catch (IOException e) {
            log.error("❌ [SSE Broadcast] 전송 실패: callId={}, event={}, error={}",
                    callId, eventName, e.getMessage(), e);
            emitters.remove(callId, emitter);
        }
    }

    /**
     * 현재 활성 SSE 연결 수 조회 (디버깅용)
     */
    public int getActiveConnectionCount() {
        return emitters.size();
    }
}
