import apiClient from './index';

export interface OcrDocumentResponse {
    text?: string;
    medication?: {
        name?: string;
        dosage?: string;
        times?: string[];
        instructions?: string;
    };
    raw?: any;
}

export interface MedicationCandidate {
    medication_name: string;
    dosage?: string;
    times: string[];
    instructions?: string;
    confidence: number;
    category?: string;
    item_seq?: string;
    entp_name?: string;
    item_ingr_name?: string;
    spclty_pblc?: string;
    prduct_type?: string;
    match_score?: number;
    match_method?: string;
    purpose?: string;
    caution?: string;
    evidence?: Record<string, unknown>;
    validation_messages?: string[];
}

export interface MedicationValidationResult {
    success: boolean;
    medications: MedicationCandidate[];
    raw_ocr_text: string;
    llm_analysis?: string;
    warnings: string[];
    error_message?: string;
    decision_status?: string;
    match_confidence?: number;
    requires_user_confirmation?: boolean;
    decision_reasons?: string[];
    request_id?: string;
}

export interface ConfirmMedicationRequest {
    requestId: string;
    selectedItemSeq: string;
    confirmed: boolean;
}

export interface ConfirmMedicationResponse {
    success: boolean;
    message: string;
    alias_suggestion_created?: boolean;
}

export interface PendingConfirmationItem {
    request_id: string;
    raw_ocr_text: string;
    decision_status: string;
    match_confidence: number;
    best_drug_name?: string;
    best_drug_item_seq?: string;
    candidates: Record<string, unknown>[];
    created_at?: string;
}

export const analyzeDocument = async (imageFile: File): Promise<OcrDocumentResponse> => {
    const formData = new FormData();
    formData.append('file', imageFile);

    const response = await apiClient.post('/api/ocr/document-ai', formData, {
        headers: {
            'Content-Type': 'multipart/form-data',
        },
        timeout: 60000,
    });

    const rawData = response.data;
    let extractedText = '';
    let medication = undefined;

    if (typeof rawData === 'string') {
        extractedText = rawData;
    } else if (rawData) {
        extractedText = rawData.text
            || rawData.content
            || rawData.result
            || rawData.extracted_text
            || rawData.ocr_text
            || (typeof rawData === 'object' ? JSON.stringify(rawData, null, 2) : String(rawData));

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

export const validateMedicationOCR = async (
    ocrText: string,
    elderlyUserId?: number,
): Promise<MedicationValidationResult> => {
    const response = await apiClient.post<MedicationValidationResult>('/api/ocr/validate-medication', {
        ocrText,
        elderlyUserId,
    });
    return response.data;
};

export const confirmMedication = async (
    request: ConfirmMedicationRequest,
): Promise<ConfirmMedicationResponse> => {
    const response = await apiClient.post<ConfirmMedicationResponse>('/api/ocr/confirm-medication', request);
    return response.data;
};

export const getPendingConfirmations = async (
    elderlyUserId: number,
): Promise<PendingConfirmationItem[]> => {
    const response = await apiClient.get<PendingConfirmationItem[]>(`/api/ocr/pending-confirmations/${elderlyUserId}`);
    return response.data;
};

export const base64ToFile = (base64String: string, fileName: string = 'image.jpg'): File => {
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
    validateMedicationOCR,
    confirmMedication,
    getPendingConfirmations,
    base64ToFile,
};
