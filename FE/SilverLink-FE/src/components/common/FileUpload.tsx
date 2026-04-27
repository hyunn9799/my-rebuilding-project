import { useState, useRef, useCallback } from "react";
import { Upload, X, File, FileText, Image, Loader2, Download } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import { useToast } from "@/hooks/use-toast";
import apiClient from "@/api/index";

interface UploadedFile {
    id: number;
    name: string;
    size: number;
    mimeType: string;
    url: string;
}

interface FileUploadProps {
    value?: UploadedFile[];
    onChange?: (files: UploadedFile[]) => void;
    maxFiles?: number;
    maxSizeBytes?: number;
    acceptedTypes?: string[];
    disabled?: boolean;
}

const FileUpload = ({
    value = [],
    onChange,
    maxFiles = 5,
    maxSizeBytes = 10 * 1024 * 1024, // 10MB
    acceptedTypes = [".pdf", ".doc", ".docx", ".xls", ".xlsx", ".jpg", ".png", ".gif"],
    disabled = false,
}: FileUploadProps) => {
    const { toast } = useToast();
    const fileInputRef = useRef<HTMLInputElement>(null);
    const [isUploading, setIsUploading] = useState(false);
    const [uploadProgress, setUploadProgress] = useState(0);

    const formatFileSize = (bytes: number): string => {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + " KB";
        return (bytes / (1024 * 1024)).toFixed(1) + " MB";
    };

    const getFileIcon = (mimeType: string) => {
        if (mimeType.startsWith("image/")) return <Image className="w-4 h-4" />;
        if (mimeType.includes("pdf")) return <FileText className="w-4 h-4 text-red-500" />;
        return <File className="w-4 h-4" />;
    };

    const handleFileSelect = useCallback(
        async (event: React.ChangeEvent<HTMLInputElement>) => {
            const files = event.target.files;
            if (!files || files.length === 0) return;

            // 최대 파일 수 체크
            if (value.length + files.length > maxFiles) {
                toast({
                    title: "파일 수 초과",
                    description: `최대 ${maxFiles}개의 파일만 첨부할 수 있습니다.`,
                    variant: "destructive",
                });
                return;
            }

            const newFiles: UploadedFile[] = [];

            for (const file of Array.from(files)) {
                // 파일 크기 체크
                if (file.size > maxSizeBytes) {
                    toast({
                        title: "파일 크기 초과",
                        description: `${file.name}이(가) 최대 크기(${formatFileSize(maxSizeBytes)})를 초과합니다.`,
                        variant: "destructive",
                    });
                    continue;
                }

                try {
                    setIsUploading(true);
                    setUploadProgress(0);

                    const formData = new FormData();
                    formData.append("file", file);

                    const response = await apiClient.post<{ id: number; url: string }>(
                        "/api/files/upload",
                        formData,
                        {
                            headers: { "Content-Type": "multipart/form-data" },
                            onUploadProgress: (progressEvent) => {
                                if (progressEvent.total) {
                                    setUploadProgress(
                                        Math.round((progressEvent.loaded * 100) / progressEvent.total)
                                    );
                                }
                            },
                        }
                    );

                    newFiles.push({
                        id: response.data.id,
                        name: file.name,
                        size: file.size,
                        mimeType: file.type,
                        url: response.data.url,
                    });
                } catch (error) {
                    console.error("File upload failed:", error);
                    toast({
                        title: "업로드 실패",
                        description: `${file.name} 업로드에 실패했습니다.`,
                        variant: "destructive",
                    });
                }
            }

            setIsUploading(false);
            setUploadProgress(0);

            if (newFiles.length > 0 && onChange) {
                onChange([...value, ...newFiles]);
            }

            // 입력 초기화
            if (fileInputRef.current) {
                fileInputRef.current.value = "";
            }
        },
        [value, onChange, maxFiles, maxSizeBytes, toast]
    );

    const handleRemove = (fileId: number) => {
        if (onChange) {
            onChange(value.filter((f) => f.id !== fileId));
        }
    };

    const handleDownload = (file: UploadedFile) => {
        window.open(file.url, "_blank");
    };

    return (
        <div className="space-y-3">
            {/* 업로드 영역 */}
            <div
                className={`
          border-2 border-dashed rounded-lg p-6 text-center transition-colors
          ${disabled ? "bg-muted cursor-not-allowed" : "hover:border-primary cursor-pointer"}
          ${isUploading ? "pointer-events-none opacity-60" : ""}
        `}
                onClick={() => !disabled && !isUploading && fileInputRef.current?.click()}
            >
                <input
                    ref={fileInputRef}
                    type="file"
                    multiple
                    accept={acceptedTypes.join(",")}
                    onChange={handleFileSelect}
                    className="hidden"
                    disabled={disabled || isUploading}
                />

                {isUploading ? (
                    <div className="space-y-2">
                        <Loader2 className="w-8 h-8 mx-auto animate-spin text-primary" />
                        <p className="text-sm text-muted-foreground">업로드 중...</p>
                        <Progress value={uploadProgress} className="w-full max-w-xs mx-auto" />
                    </div>
                ) : (
                    <>
                        <Upload className="w-8 h-8 mx-auto text-muted-foreground mb-2" />
                        <p className="text-sm text-muted-foreground">
                            클릭하거나 파일을 드래그하여 업로드
                        </p>
                        <p className="text-xs text-muted-foreground mt-1">
                            최대 {maxFiles}개, 각 {formatFileSize(maxSizeBytes)} 이하
                        </p>
                    </>
                )}
            </div>

            {/* 업로드된 파일 목록 */}
            {value.length > 0 && (
                <div className="space-y-2">
                    {value.map((file) => (
                        <div
                            key={file.id}
                            className="flex items-center gap-3 p-3 bg-muted/50 rounded-lg"
                        >
                            {getFileIcon(file.mimeType)}
                            <div className="flex-1 min-w-0">
                                <p className="text-sm font-medium truncate">{file.name}</p>
                                <p className="text-xs text-muted-foreground">
                                    {formatFileSize(file.size)}
                                </p>
                            </div>
                            <div className="flex items-center gap-1">
                                <Button
                                    variant="ghost"
                                    size="icon"
                                    className="h-8 w-8"
                                    onClick={() => handleDownload(file)}
                                >
                                    <Download className="w-4 h-4" />
                                </Button>
                                {!disabled && (
                                    <Button
                                        variant="ghost"
                                        size="icon"
                                        className="h-8 w-8 text-destructive hover:text-destructive"
                                        onClick={() => handleRemove(file.id)}
                                    >
                                        <X className="w-4 h-4" />
                                    </Button>
                                )}
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
};

export default FileUpload;
