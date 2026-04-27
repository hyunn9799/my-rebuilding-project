package com.aicc.silverlink.infra.external.luxia;

import com.aicc.silverlink.global.exception.LuxiaHttpException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.Map;

@Slf4j
@Component
public class LuxiaDocumentAiClient {

    private final WebClient webClient;
    private final LuxiaProperties props;

    public LuxiaDocumentAiClient(
            @Qualifier("luxiaWebClient") WebClient webClient,
            LuxiaProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    public Map<String, Object> callDocumentAi(MultipartFile file) {
        validateImage(file);

        String contentType = (file.getContentType() != null) ? file.getContentType() : "image/png";

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception e) {
            throw new RuntimeException("failed to read file bytes", e);
        }

        // 📊 이미지 크기 로깅
        log.info("[Luxia OCR] Image size: {} bytes ({} KB), contentType: {}",
                bytes.length, bytes.length / 1024, contentType);

        String b64 = Base64.getEncoder().encodeToString(bytes);
        String dataUrl = "data:" + contentType + ";base64," + b64;

        // 📊 Base64 크기 로깅
        log.info("[Luxia OCR] Base64 payload size: {} chars ({} KB)",
                dataUrl.length(), dataUrl.length() / 1024);

        Map<String, String> req = Map.of("image", dataUrl);

        log.info("[Luxia OCR] Calling Luxia API: {}{}", props.baseUrl(), props.documentAi().path());

        Map<String, Object> res = webClient.post()
                .uri(props.documentAi().path())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .retrieve()
                .onStatus(
                        s -> s.is4xxClientError() || s.is5xxServerError(),
                        resp -> resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> Mono.error(new LuxiaHttpException(resp.statusCode().value(), body))))
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .block();

        if (res == null)
            throw new LuxiaHttpException(502, "Empty response from LUXIA");

        log.info("[Luxia OCR] Success! Response keys: {}", res.keySet());
        return res;
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty())
            throw new IllegalArgumentException("file is empty");
        String ct = file.getContentType();
        if (ct == null || !ct.startsWith("image/")) {
            throw new IllegalArgumentException("file is not image. contentType=" + ct);
        }
    }
}
