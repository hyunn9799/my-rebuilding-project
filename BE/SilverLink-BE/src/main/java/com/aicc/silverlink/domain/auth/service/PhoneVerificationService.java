package com.aicc.silverlink.domain.auth.service;

import com.aicc.silverlink.domain.auth.dto.PhoneVerificationDtos;
import com.aicc.silverlink.domain.auth.entity.PhoneVerification;
import com.aicc.silverlink.domain.auth.repository.PhoneVerificationRepository;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.repository.UserRepository;
import com.aicc.silverlink.global.config.auth.AuthPhoneProperties;
import com.aicc.silverlink.infra.external.sms.TwilioSmsSender;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class PhoneVerificationService {

    private final PhoneVerificationRepository repo;
    private final UserRepository userRepo;
    private final StringRedisTemplate redis;
    private final TwilioSmsSender twilioSmsSender;
    private final AuthPhoneProperties props;

    /**
     * 인증번호 요청 (Twilio Verify API가 자동 생성/발송)
     */
    @Transactional
    public PhoneVerificationDtos.RequestCodeResponse requestCode(PhoneVerificationDtos.RequestCodeRequest req,
            String ip) {
        String phoneE164 = toE164Kr(req.phone());

        // 쿨다운 체크 (동일 번호로 연속 요청 방지)
        String cooldownKey = "pv:cooldown:" + phoneE164 + ":" + req.purpose();
        if (Boolean.TRUE.equals(redis.hasKey(cooldownKey))) {
            throw new IllegalArgumentException("PHONE_COOLDOWN");
        }

        // 일일 횟수 제한
        String dailyKey = "pv:daily:" + phoneE164 + ":" + req.purpose() + ":" + LocalDateTime.now().toLocalDate();
        Long dailyCount = redis.opsForValue().increment(dailyKey);
        if (dailyCount != null && dailyCount == 1) {
            redis.expire(dailyKey, 2, TimeUnit.DAYS);
        }
        if (dailyCount != null && dailyCount > props.getDailyLimit()) {
            throw new IllegalArgumentException("PHONE_DAILY_LIMIT");
        }

        // 쿨다운 설정
        redis.opsForValue().set(cooldownKey, "1", props.getCooldownSeconds(), TimeUnit.SECONDS);

        // 사용자 조회 (선택적)
        User user = null;
        if (req.userId() != null) {
            user = userRepo.findById(req.userId())
                    .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));
        }

        // Twilio Verify API로 인증번호 발송 (Twilio가 자동 생성)
        boolean sent = twilioSmsSender.sendVerificationCode(phoneE164);
        if (!sent) {
            throw new IllegalStateException("SMS_SEND_FAILED");
        }

        // DB에 요청 기록 저장 (codeHash는 Twilio가 관리하므로 null)
        PhoneVerification pv = PhoneVerification.create(
                user,
                phoneE164,
                req.purpose(),
                null, // Twilio가 코드를 관리하므로 codeHash 불필요
                ip,
                props.getTtlSeconds());

        repo.save(pv);

        // 남은 시간(초)을 서버에서 계산하여 반환 - 클라이언트 시간대 불일치 문제 방지
        long expiresInSeconds = java.time.Duration.between(
                LocalDateTime.now(), pv.getExpiresAt()).getSeconds();

        return new PhoneVerificationDtos.RequestCodeResponse(
                pv.getId(),
                pv.getExpiresAt(),
                Math.max(expiresInSeconds, 0), // 음수 방지
                null // Twilio 방식에서는 디버그 코드 반환 불가
        );
    }

    /**
     * 인증번호 검증 (Twilio Verify API로 검증)
     */
    @Transactional
    public PhoneVerificationDtos.VerifyCodeResponse verifyCode(PhoneVerificationDtos.VerifyCodeRequest req, String ip) {
        PhoneVerification pv = repo.findById(req.verificationId())
                .orElseThrow(() -> new IllegalArgumentException("PV_NOT_FOUND"));

        if (pv.getStatus() != PhoneVerification.Status.REQUESTED) {
            throw new IllegalArgumentException("PV_NOT_REQUESTED");
        }

        // 시간 비교는 Twilio Verify API에 위임 (서버 시간대 불일치 문제 방지)
        // Twilio가 EXPIRED를 반환하면 switch 문에서 처리됨

        if (pv.getFailCount() >= props.getMaxAttemps()) {
            pv.fail();
            throw new IllegalArgumentException("PV_TOO_MANY_ATTEMPTS");
        }

        // Twilio Verify API로 검증
        TwilioSmsSender.VerificationResult result = twilioSmsSender.checkVerificationCode(
                pv.getPhoneE164(),
                req.code());

        switch (result) {
            case SUCCESS:
                pv.verify();
                break;

            case CODE_MISMATCH:
                pv.increaseFailCount();
                if (pv.getFailCount() >= props.getMaxAttemps()) {
                    pv.fail();
                }
                throw new IllegalArgumentException("PV_CODE_INVALID");

            case EXPIRED:
                pv.expire();
                throw new IllegalArgumentException("PV_EXPIRED");
        }

        // 사용자가 연결된 경우 전화번호 인증 완료 마킹
        if (pv.getUser() != null) {
            User user = pv.getUser();
            user.markPhoneVerified();
        }

        // 증명 토큰 생성 (이후 회원가입 등에 사용)
        String proofToken = UUID.randomUUID().toString();
        String proofKey = "pv:proof:" + proofToken;

        redis.opsForValue().set(
                proofKey,
                pv.getPhoneE164(),
                5,
                TimeUnit.MINUTES);

        return new PhoneVerificationDtos.VerifyCodeResponse(true, proofToken);
    }

    /**
     * 전화번호를 E.164 형식으로 변환 (한국 번호)
     */
    private String toE164Kr(String raw) {
        String digits = raw.replaceAll("[^0-9+]", "");
        if (digits.startsWith("+"))
            return digits;
        if (digits.startsWith("0")) {
            String no0 = digits.substring(1);
            return "+82" + no0;
        }
        return "+82" + digits;
    }
}
