import apiClient from './index';

/**
 * OCR API 모듈
 * 백엔드: OcrController (/api/ocr)
 */

export interface OcrDocumentResponse {
    text?: string;  // 추출된 텍스트
    medication?: {  // 복약 정보 (약봉투 인식 시)
        name?: string;
        dosage?: string;
        times?: string[];
        instructions?: string;
    };
    raw?: any;  // 원본 응답
}

/**
 * 문서 이미지 OCR 분석
 * POST /api/ocr/document-ai
 * @param imageFile 이미지 파일
 */
export const analyzeDocument = async (imageFile: File): Promise<OcrDocumentResponse> => {
    const formData = new FormData();
    formData.append('file', imageFile);

    const response = await apiClient.post('/api/ocr/document-ai', formData, {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
        timeout: 60000, // OCR은 처리 시간이 오래 걸릴 수 있으므로 60초로 설정
    });

    // Luxia AI 응답 파싱
    const rawData = response.data;

    // text 필드 추출 (응답 구조에 따라 조정 필요)
    let extractedText = '';
    let medication = undefined;

    // 응답이 문자열인 경우
    if (typeof rawData === 'string') {
        extractedText = rawData;
    }
    // 응답이 객체인 경우
    else if (rawData) {
        // 여러 가지 가능한 필드 이름 시도
        extractedText = rawData.text
            || rawData.content
            || rawData.result
            || rawData.extracted_text
            || rawData.ocr_text
            || (typeof rawData === 'object' ? JSON.stringify(rawData, null, 2) : String(rawData));

        // 약봉투 분석 결과가 있는 경우
        if (rawData.medication || rawData.medicine) {
            const med = rawData.medication || rawData.medicine;
            medication = {
                name: med.name || med.medicine_name,
                dosage: med.dosage || med.dose,
                times: med.times || med.schedule || [],
                instructions: med.instructions || med.usage || med.notes,
            };
        }
    }

    return {
        text: extractedText,
        medication,
        raw: rawData,
    };
};

/**
 * Base64 이미지를 File로 변환
 */
export const base64ToFile = (base64String: string, fileName: string = 'image.jpg'): File => {
    // data:image/jpeg;base64,... 형식에서 순수 base64 추출
    const parts = base64String.split(',');
    const mimeMatch = parts[0].match(/:(.*?);/);
    const mimeType = mimeMatch ? mimeMatch[1] : 'image/jpeg';
    const base64Data = parts.length > 1 ? parts[1] : parts[0];

    const byteCharacters = atob(base64Data);
    const byteNumbers = new Array(byteCharacters.length);
    for (let i = 0; i < byteCharacters.length; i++) {
        byteNumbers[i] = byteCharacters.charCodeAt(i);
    }
    const byteArray = new Uint8Array(byteNumbers);

    return new File([byteArray], fileName, { type: mimeType });
};

export default {
    analyzeDocument,
    base64ToFile,
};
