package com.aicc.silverlink.infra.external.luxia.dto;

public record LuxiaDocumentAiRequest(
        String image,
        String model // optional (문서 예시엔 없어도 됨)
) {}

