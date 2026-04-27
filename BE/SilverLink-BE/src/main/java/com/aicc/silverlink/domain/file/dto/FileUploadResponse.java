package com.aicc.silverlink.domain.file.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 파일 업로드 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {

    private String fileName; // 저장된 파일명 (UUID)
    private String originalFileName; // 원본 파일명
    private String fileUrl; // 파일 접근 URL (S3 URL)
    private String filePath; // S3 내 경로
    private Long fileSize; // 파일 크기 (bytes)
    private String contentType; // MIME 타입
}
