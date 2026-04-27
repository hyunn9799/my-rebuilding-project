package com.aicc.silverlink.domain.auth.repository;

import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.repository.UserRepository;
import com.yubico.webauthn.CredentialRepository;
import com.yubico.webauthn.RegisteredCredential;
import com.yubico.webauthn.data.ByteArray;
import com.yubico.webauthn.data.PublicKeyCredentialDescriptor;
import com.yubico.webauthn.data.PublicKeyCredentialType;
import com.yubico.webauthn.data.exception.Base64UrlException;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class YubicoCredentialRepository implements CredentialRepository {

    private final UserRepository userRepo;
    private final WebAuthnCredentialRepository credRepo;

    @Override
    public Set<PublicKeyCredentialDescriptor> getCredentialIdsForUsername(String username) {
        User user = userRepo.findByLoginId(username).orElse(null);
        if (user == null) return Set.of();

        return credRepo.findAllByUser_IdAndRevokedAtIsNull(user.getId()).stream()
                .map(c -> PublicKeyCredentialDescriptor.builder()
                        .id(ByteArray.fromBase64(c.getCredentialId()))
                        .type(PublicKeyCredentialType.PUBLIC_KEY)
                        .build()
                )
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<ByteArray> getUserHandleForUsername(String username) {
        return userRepo.findByLoginId(username).map(u-> toUserHandle(u.getId()));
    }

    @Override
    public Optional<String> getUsernameForUserHandle(ByteArray userHandle) {
        Long userId = fromUserHandle(userHandle);
        return userRepo.findById(userId).map(User::getLoginId);
    }

    @Override
    public Optional<RegisteredCredential> lookup(ByteArray credentialId, ByteArray userHandle) {
        Long userId = fromUserHandle(userHandle);
        String credIdB64 = credentialId.getBase64Url();

        return credRepo.findByCredentialIdAndUser_Id(credIdB64,userId)
                .map(c-> {
                    try {
                        return RegisteredCredential.builder()
                                .credentialId(ByteArray.fromBase64Url(c.getCredentialId()))
                                .userHandle(toUserHandle(userId))
                                .publicKeyCose(new ByteArray(c.getPublicKey()))
                                .signatureCount((long) c.getSignCount())
                                .build();
                    } catch (Base64UrlException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @Override
    public Set<RegisteredCredential> lookupAll(ByteArray credentialId) {
        String credIdB64 = credentialId.getBase64Url();
        return credRepo.findByCredentialId(credIdB64)
                .map(c->{
                    Long userId = c.getUser().getId();
                    try {
                        return Set.of(RegisteredCredential.builder()
                                .credentialId(ByteArray.fromBase64Url(c.getCredentialId()))
                                .userHandle(toUserHandle(userId))
                                .publicKeyCose(new ByteArray(c.getPublicKey()))
                                .signatureCount((long) c.getSignCount())
                                .build());
                    } catch (Base64UrlException e) {
                        throw new RuntimeException(e);
                    }
                })
                .orElse(Set.of());
    }

    private ByteArray toUserHandle(Long userId) {
        byte[] b = ByteBuffer.allocate(Long.BYTES).putLong(userId).array();
        return new ByteArray(b);
    }

    private Long fromUserHandle(ByteArray userHandle) {
        ByteBuffer bb = ByteBuffer.wrap(userHandle.getBytes());
        return bb.getLong();
    }
}
