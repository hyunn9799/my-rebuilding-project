package com.aicc.silverlink.domain.file.service;

import com.aicc.silverlink.domain.file.dto.FileUploadResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 파일 업로드 서비스
 * - S3 설정 시: AWS S3에 업로드
 * - S3 미설정 시: 로컬 파일 시스템에 저장
 */
@Slf4j
@Service
public class FileService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final boolean s3Enabled;

    @Value("${cloud.aws.s3.bucket:silverlink-files}")
    private String bucketName;

    @Value("${cloud.aws.region.static:ap-northeast-2}")
    private String region;

    @Value("${file.upload.local-path:./uploads}")
    private String localUploadPath;

    @Autowired
    public FileService(
            @Autowired(required = false) S3Client s3Client,
            @Autowired(required = false) S3Presigner s3Presigner) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.s3Enabled = (s3Client != null && s3Presigner != null);
        log.info("FileService initialized - S3 enabled: {}", this.s3Enabled);
    }

    /**
     * 단일 파일 업로드
     */
    public FileUploadResponse uploadFile(MultipartFile file, String directory) {
        if (s3Enabled) {
            return uploadToS3(file, directory);
        } else {
            return uploadToLocal(file, directory);
        }
    }

    /**
     * 다중 파일 업로드
     */
    public List<FileUploadResponse> uploadFiles(MultipartFile[] files, String directory) {
        List<FileUploadResponse> responses = new ArrayList<>();
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                responses.add(uploadFile(file, directory));
            }
        }
        return responses;
    }

    /**
     * 파일 삭제
     */
    public void deleteFile(String filePath) {
        if (s3Enabled) {
            deleteFromS3(filePath);
        } else {
            deleteFromLocal(filePath);
        }
    }

    /**
     * 파일 URL 조회 (S3 공개 URL)
     */
    public String getPresignedUrl(String filePath) {
        if (s3Enabled) {
            return String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, filePath);
        } else {
            return "/uploads/" + filePath;
        }
    }

    /**
     * S3 Pre-signed URL 생성 (1시간 유효)
     * 
     * @param s3Url s3://bucket-name/key 형식 또는 key 직접 전달
     * @return Pre-signed URL (브라우저에서 접근 가능)
     */
    public String generatePresignedUrl(String s3Url) {
        if (s3Url == null || s3Url.isBlank()) {
            log.debug("generatePresignedUrl: s3Url is null or blank");
            return null;
        }

        // S3가 비활성화된 경우, 원본 URL을 HTTPS 형식으로 변환하여 반환
        if (!s3Enabled) {
            log.info("S3 is not enabled, converting s3:// URL to https:// format. Original: {}", s3Url);
            if (s3Url.startsWith("s3://")) {
                // s3://bucket-name/key -> https://bucket-name.s3.region.amazonaws.com/key
                String path = s3Url.substring(5);
                int slashIndex = path.indexOf('/');
                if (slashIndex != -1) {
                    String bucket = path.substring(0, slashIndex);
                    String key = path.substring(slashIndex + 1);
                    String httpsUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
                    log.info("Converted to: {}", httpsUrl);
                    return httpsUrl;
                }
            }
            return s3Url; // 변환 불가능하면 원본 반환
        }

        log.debug("generatePresignedUrl: s3Enabled={}, s3Url={}", s3Enabled, s3Url);

        try {
            String bucket;
            String key;

            // s3://bucket-name/key 형식 파싱
            if (s3Url.startsWith("s3://")) {
                String path = s3Url.substring(5); // "s3://" 제거
                int slashIndex = path.indexOf('/');
                if (slashIndex == -1) {
                    log.warn("Invalid S3 URL format: {}", s3Url);
                    return null;
                }
                bucket = path.substring(0, slashIndex);
                key = path.substring(slashIndex + 1);
            } else {
                // key만 전달된 경우
                bucket = bucketName;
                key = s3Url;
            }

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofHours(1)) // 1시간 유효
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            String presignedUrl = presignedRequest.url().toString();

            log.debug("Generated Pre-signed URL for: {}", s3Url);
            return presignedUrl;

        } catch (Exception e) {
            log.error("Failed to generate Pre-signed URL for: {}", s3Url, e);
            return null;
        }
    }

    /**
     * 파일 리소스 로드
     */
    public org.springframework.core.io.Resource loadFileAsResource(String filePath) {
        try {
            if (s3Enabled) {
                // S3에서 파일 다운로드
                software.amazon.awssdk.services.s3.model.GetObjectRequest getRequest = software.amazon.awssdk.services.s3.model.GetObjectRequest
                        .builder()
                        .bucket(bucketName)
                        .key(filePath)
                        .build();

                byte[] fileBytes = s3Client.getObject(getRequest).readAllBytes();
                return new org.springframework.core.io.ByteArrayResource(fileBytes);
            } else {
                // 로컬 파일 시스템에서 파일 로드
                Path file = Paths.get(localUploadPath).resolve(filePath).normalize();
                org.springframework.core.io.Resource resource = new org.springframework.core.io.UrlResource(
                        file.toUri());

                if (resource.exists() && resource.isReadable()) {
                    return resource;
                } else {
                    throw new RuntimeException("파일을 찾을 수 없거나 읽을 수 없습니다: " + filePath);
                }
            }
        } catch (Exception e) {
            log.error("파일 로드 실패: {}", filePath, e);
            throw new RuntimeException("파일 로드에 실패했습니다: " + filePath, e);
        }
    }

    /**
     * 파일 다운로드 (byte[] 반환)
     */
    public byte[] downloadFile(String filePath) {
        try {
            if (s3Enabled) {
                GetObjectRequest getRequest = GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(filePath)
                        .build();
                return s3Client.getObject(getRequest).readAllBytes();
            } else {
                Path file = Paths.get(localUploadPath).resolve(filePath).normalize();
                return Files.readAllBytes(file);
            }
        } catch (Exception e) {
            log.error("파일 다운로드 실패: {}", filePath, e);
            throw new RuntimeException("파일 다운로드에 실패했습니다: " + filePath, e);
        }
    }

    // ==================== S3 Methods ====================

    private FileUploadResponse uploadToS3(MultipartFile file, String directory) {
        String originalFileName = file.getOriginalFilename();
        String extension = getExtension(originalFileName);
        String fileName = UUID.randomUUID().toString() + extension;
        String filePath = directory + "/" + fileName;

        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filePath)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            String fileUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, filePath);
            log.info("File uploaded to S3: {}", fileUrl);

            return FileUploadResponse.builder()
                    .fileName(fileName)
                    .originalFileName(originalFileName)
                    .fileUrl(fileUrl)
                    .filePath(filePath)
                    .fileSize(file.getSize())
                    .contentType(file.getContentType())
                    .build();

        } catch (IOException e) {
            log.error("Failed to upload file to S3: {}", originalFileName, e);
            throw new RuntimeException("파일 업로드에 실패했습니다: " + originalFileName, e);
        }
    }

    private void deleteFromS3(String filePath) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(filePath)
                    .build();
            s3Client.deleteObject(deleteRequest);
            log.info("File deleted from S3: {}", filePath);
        } catch (Exception e) {
            log.error("Failed to delete file from S3: {}", filePath, e);
            throw new RuntimeException("파일 삭제에 실패했습니다: " + filePath, e);
        }
    }

    // ==================== Local Methods ====================

    private FileUploadResponse uploadToLocal(MultipartFile file, String directory) {
        String originalFileName = file.getOriginalFilename();
        String extension = getExtension(originalFileName);
        String fileName = UUID.randomUUID().toString() + extension;
        String filePath = directory + "/" + fileName;

        try {
            Path uploadDir = Paths.get(localUploadPath, directory);
            Files.createDirectories(uploadDir);

            Path targetPath = uploadDir.resolve(fileName);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            String fileUrl = "/uploads/" + filePath;
            log.info("File uploaded to local: {}", targetPath);

            return FileUploadResponse.builder()
                    .fileName(fileName)
                    .originalFileName(originalFileName)
                    .fileUrl(fileUrl)
                    .filePath(filePath)
                    .fileSize(file.getSize())
                    .contentType(file.getContentType())
                    .build();

        } catch (IOException e) {
            log.error("Failed to upload file to local: {}", originalFileName, e);
            throw new RuntimeException("파일 업로드에 실패했습니다: " + originalFileName, e);
        }
    }

    private void deleteFromLocal(String filePath) {
        try {
            Path targetPath = Paths.get(localUploadPath, filePath);
            Files.deleteIfExists(targetPath);
            log.info("File deleted from local: {}", targetPath);
        } catch (IOException e) {
            log.error("Failed to delete file from local: {}", filePath, e);
            throw new RuntimeException("파일 삭제에 실패했습니다: " + filePath, e);
        }
    }

    // ==================== Utility ====================

    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }
}
