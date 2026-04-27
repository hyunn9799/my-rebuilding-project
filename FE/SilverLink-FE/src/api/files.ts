import apiClient from './index';

/**
 * 파일 업로드 API 모듈
 * 백엔드: FileController (/api/files)
 */

export interface FileUploadResponse {
    fileName: string;         // 저장된 파일명 (UUID)
    originalFileName: string; // 원본 파일명
    fileUrl: string;          // 파일 접근 URL (S3 URL)
    filePath: string;         // S3 내 경로
    fileSize: number;         // 파일 크기 (bytes)
    contentType: string;      // MIME 타입
}

/**
 * 단일 파일 업로드
 * POST /api/files/upload
 */
export const uploadFile = async (
    file: File,
    directory: string = 'uploads'
): Promise<FileUploadResponse> => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('directory', directory);

    const response = await apiClient.post<FileUploadResponse>('/api/files/upload', formData, {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
    });
    return response.data;
};

/**
 * 다중 파일 업로드
 * POST /api/files/upload-multiple
 */
export const uploadFiles = async (
    files: File[],
    directory: string = 'uploads'
): Promise<FileUploadResponse[]> => {
    const formData = new FormData();
    files.forEach((file) => {
        formData.append('files', file);
    });
    formData.append('directory', directory);

    const response = await apiClient.post<FileUploadResponse[]>('/api/files/upload-multiple', formData, {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
    });
    return response.data;
};

/**
 * 파일 삭제
 * DELETE /api/files?filePath=xxx
 */
export const deleteFile = async (filePath: string): Promise<void> => {
    await apiClient.delete('/api/files', {
        params: { filePath },
    });
};

/**
 * 파일 URL 조회
 * GET /api/files/url?filePath=xxx
 */
export const getFileUrl = async (filePath: string): Promise<string> => {
    const response = await apiClient.get<string>('/api/files/url', {
        params: { filePath },
    });
    return response.data;
};

export default {
    uploadFile,
    uploadFiles,
    deleteFile,
    getFileUrl,
};
