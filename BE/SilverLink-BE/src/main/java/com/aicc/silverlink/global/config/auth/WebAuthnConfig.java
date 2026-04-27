package com.aicc.silverlink.global.config.auth;

import com.aicc.silverlink.domain.auth.repository.YubicoCredentialRepository;
import com.yubico.webauthn.RelyingParty;
import com.yubico.webauthn.data.RelyingPartyIdentity;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class WebAuthnConfig {

    private final WebAuthnProperties props;
    private final YubicoCredentialRepository credentialRepository;

    @Bean
    public RelyingParty relyingParty(){
        RelyingPartyIdentity rpIdentity = RelyingPartyIdentity.builder()
                .id(props.getRpId())
                .name(props.getRpName())
                .build();

        return RelyingParty.builder()
                .identity(rpIdentity)
                .credentialRepository(credentialRepository)
                .origins(props.getOrigins())
                .build();

    }


}
