package com.aicc.silverlink.global.config.redis;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Redis 키 네이밍 표준화 매니저.
 * 환경별(dev/staging/prod) 접두사를 자동 적용하여 키 충돌을 방지합니다.
 *
 * <p>
 * 키 형식: {@code {env}:sl:{domain}:{key}}
 * 
 * <pre>
 * 예시:
 *   prod:sl:sess:{sid}
 *   dev:sl:pv:cooldown:{phone}
 *   prod:sl:login:pending:{token}
 * </pre>
 */
@Component
public class RedisKeyManager {

    private final String env;
    private static final String APP_PREFIX = "sl";

    public RedisKeyManager(@Value("${spring.profiles.active:dev}") String env) {
        this.env = env;
    }

    /**
     * 표준화된 Redis 키를 생성합니다.
     *
     * @param domain 도메인 영역 (예: "sess", "pv", "login")
     * @param parts  키 구성 요소들
     * @return 표준화된 키 (예: "prod:sl:sess:abc-123")
     */
    public String key(String domain, String... parts) {
        StringBuilder sb = new StringBuilder();
        sb.append(env).append(':').append(APP_PREFIX).append(':').append(domain);
        for (String part : parts) {
            sb.append(':').append(part);
        }
        return sb.toString();
    }

    /**
     * 도메인별 SCAN 패턴을 반환합니다.
     * 예: "prod:sl:sess:*"
     */
    public String scanPattern(String domain) {
        return env + ":" + APP_PREFIX + ":" + domain + ":*";
    }

    /**
     * 현재 환경의 모든 키 SCAN 패턴.
     * 예: "dev:sl:*"
     */
    public String envPattern() {
        return env + ":" + APP_PREFIX + ":*";
    }
}
