package com.aicc.silverlink.domain.ocr;

import com.aicc.silverlink.infra.external.luxia.LuxiaDocumentAiClient;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Map;

@Tag(name = "OCR", description = "문서 인식 API")
@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
public class OcrController {

    private final LuxiaDocumentAiClient client;

    @PostMapping(value = "/document-ai", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> documentAi(@RequestPart("file") @NotNull MultipartFile file) {
        return client.callDocumentAi(file);
    }
}
