package com.aicc.silverlink.domain.file.controller;

import com.aicc.silverlink.domain.file.dto.FileUploadResponse;
import com.aicc.silverlink.domain.file.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import java.util.List;

@Tag(name = "파일", description = "파일 업로드/다운로드 API")
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    /**
     * 단일 파일 업로드
     * POST /api/files/upload
     */
    @Operation(summary = "단일 파일 업로드", description = "파일을 AWS S3에 업로드합니다.")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FileUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "directory", defaultValue = "uploads") String directory) {
        FileUploadResponse response = fileService.uploadFile(file, directory);
        return ResponseEntity.ok(response);
    }

    /**
     * 다중 파일 업로드
     * POST /api/files/upload-multiple
     */
    @Operation(summary = "다중 파일 업로드", description = "여러 파일을 AWS S3에 업로드합니다.")
    @PostMapping(value = "/upload-multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<FileUploadResponse>> uploadFiles(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(value = "directory", defaultValue = "uploads") String directory) {
        List<FileUploadResponse> responses = fileService.uploadFiles(files, directory);
        return ResponseEntity.ok(responses);
    }

    /**
     * 파일 삭제
     * DELETE /api/files/{filePath}
     */
    @Operation(summary = "파일 삭제", description = "S3에서 파일을 삭제합니다.")
    @DeleteMapping
    public ResponseEntity<Void> deleteFile(@RequestParam("filePath") String filePath) {
        fileService.deleteFile(filePath);
        return ResponseEntity.ok().build();
    }

    /**
     * 파일 URL 조회
     * GET /api/files/url
     */
    @Operation(summary = "파일 URL 조회", description = "파일의 다운로드 URL을 반환합니다.")
    @GetMapping("/url")
    public ResponseEntity<String> getFileUrl(@RequestParam("filePath") String filePath) {
        String url = fileService.getPresignedUrl(filePath);
        return ResponseEntity.ok(url);
    }

    /**
     * 파일 다운로드
     * GET /api/files/download
     */
    @Operation(summary = "파일 다운로드", description = "파일을 읽어와서 다운로드 처리합니다.")
    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadFile(
            @RequestParam("filePath") String filePath,
            @RequestParam("originalFileName") String originalFileName) {

        byte[] fileData = fileService.downloadFile(filePath);
        String encodedFileName = URLEncoder.encode(originalFileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFileName + "\"")
                .body(fileData);
    }
}
