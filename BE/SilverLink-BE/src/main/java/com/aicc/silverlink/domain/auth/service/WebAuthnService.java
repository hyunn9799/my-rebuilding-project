package com.aicc.silverlink.domain.auth.service;

import com.aicc.silverlink.domain.auth.entity.WebAuthnCredential;
import com.aicc.silverlink.domain.auth.repository.WebAuthnCredentialRepository;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.yubico.webauthn.*;
import com.yubico.webauthn.data.*;
import com.yubico.webauthn.exception.AssertionFailedException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.sql.Time;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class WebAuthnService {

    private final RelyingParty rp;
    private final StringRedisTemplate redis;
    private final UserRepository userRepo;
    private final WebAuthnCredentialRepository credRepo;

    private static final long REQUEST_TTL_MIN = 5;

    public record StartRegResponse(String requestId, String creationOptionsJson) {}
    public record StartAuthResponse(String requsetId, String assertionRequestJson) {}


    public StartRegResponse startRegistration(Long userId) throws JsonProcessingException {
        User user = userRepo.findById(userId).orElseThrow(()-> new IllegalArgumentException("USER_NOT_FOUND"));

        byte[] handle = java.nio.ByteBuffer.allocate(Long.BYTES).putLong(userId).array();

        UserIdentity userIdentity = UserIdentity.builder()
                .name(user.getLoginId())
                .displayName(user.getName())
                .id(new ByteArray(handle))
                .build();

        PublicKeyCredentialCreationOptions options =
                rp.startRegistration(StartRegistrationOptions.builder()
                        .user(userIdentity)
                        .authenticatorSelection(AuthenticatorSelectionCriteria.builder()
                                .residentKey(ResidentKeyRequirement.PREFERRED)
                                .userVerification(UserVerificationRequirement.PREFERRED)
                                .build())
                        .build());

        String requestId = UUID.randomUUID().toString();
        redis.opsForValue().set(regKey(requestId), options.toJson(), REQUEST_TTL_MIN, TimeUnit.MINUTES);

        return new StartRegResponse(requestId, options.toJson());
    }

    @Transactional
    public void finishRegistration(Long userId, String requestId, String responseJson, Long registeredByUserId) {
        String optionsJson = redis.opsForValue().get(regKey(requestId));
        if(optionsJson == null) throw new IllegalArgumentException("WEBAUTHN_REG_REQUEST_EXPIRED");

        try {
            PublicKeyCredentialCreationOptions request = PublicKeyCredentialCreationOptions.fromJson(optionsJson);
            var pkc = PublicKeyCredential.parseRegistrationResponseJson(responseJson);

            RegistrationResult result = rp.finishRegistration(FinishRegistrationOptions.builder()
                    .request(request)
                    .response(pkc)
                    .build());

            User user = userRepo.findById(userId)
                    .orElseThrow(()->new IllegalArgumentException("USER_NOT_FOUND"));

            User regBy = null;
            if (registeredByUserId != null) {
                regBy = userRepo.findById(registeredByUserId).orElse(null);
            }

            WebAuthnCredential credential = WebAuthnCredential.register(
                    user,
                    result.getKeyId().getId().getBase64Url(),
                    request.getRp().getId(),
                    result.getPublicKeyCose().getBytes(),
                    result.getSignatureCount(),
                    regBy
            );

            credRepo.save(credential);

            redis.delete(regKey(requestId));

        }catch (Exception e) {
            throw new IllegalArgumentException("WEBAUTHN_REGISTRATION_FAILED: " + e.getMessage());
        }

    }

    public StartAuthResponse startAssertion(String loginIdOrNull) throws JsonProcessingException {
        var builder = StartAssertionOptions.builder();

        if(loginIdOrNull != null && !loginIdOrNull.isBlank()) {
            builder.username(loginIdOrNull);
        }

        AssertionRequest request = rp.startAssertion(builder.build());

        String requestId = UUID.randomUUID().toString();
        redis.opsForValue().set(authKey(requestId),request.toJson(),REQUEST_TTL_MIN, TimeUnit.MINUTES);

        return new StartAuthResponse(requestId, request.toJson());
    }



    @Transactional
    public Long finishAssertion(String requestId, String responseJson) {
        String optionJson = redis.opsForValue().get(authKey(requestId));
        if (optionJson == null) throw new IllegalArgumentException("WEBAUTHN_AUTH_REQUEST_EXPIRED");

        try {
            AssertionRequest request = AssertionRequest.fromJson(optionJson);
            var pkc = PublicKeyCredential.parseAssertionResponseJson(responseJson);

            AssertionResult result = rp.finishAssertion(FinishAssertionOptions.builder()
                    .request(request)
                    .response(pkc)
                    .build());

            if (!result.isSuccess()) throw new IllegalArgumentException("WEBAUTHN_ASSERTION_FAILED");

            String username = result.getUsername();
            User user = userRepo.findByLoginId(username)
                    .orElseThrow(()->new IllegalArgumentException("USER_NOT_FOUND"));

            String credId = result.getCredential().getCredentialId().getBase64Url();
            WebAuthnCredential cred = credRepo.findByCredentialIdAndUser_Id(credId, user.getId())
                            .orElseThrow(()-> new IllegalArgumentException("WEBAUTHN_CRED_NOT_FOUND"));

            cred.updateUsage(result.getSignatureCount());

            redis.delete(authKey(requestId));
            return user.getId();

        } catch (Exception e) {
            throw new IllegalArgumentException("WEBAUTHN_LOGIN_FAILED: " + e.getMessage());
        }
    }


    private String regKey(String requestId) { return "webauthn:reg:" + requestId; }
    private String authKey(String requestId) { return "webauthn:auth:" + requestId; }

}
