import { useState, useEffect } from "react";
import { X, ChevronLeft, ChevronRight, Bell } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogDescription,
} from "@/components/ui/dialog";
import { Badge } from "@/components/ui/badge";
import noticesApi from "@/api/notices";

export interface PopupNotice {
    id: number;
    title: string;
    content: string;
    createdAt: string;
    priority?: "HIGH" | "NORMAL";
}

interface PopupNoticeModalProps {
    notices: PopupNotice[];
    onClose: () => void;
    onDontShowToday?: (noticeIds: number[]) => void;
    open?: boolean;
}

const STORAGE_KEY = "popup_notice_hidden_until";

/**
 * 팝업 공지 모달
 * - 여러 개의 공지를 슬라이드로 표시
 * - "오늘 하루 보지 않기" 옵션
 * - 우선순위에 따라 정렬
 */
const PopupNoticeModal = ({
    notices,
    onClose,
    onDontShowToday,
    open = true,
}: PopupNoticeModalProps) => {
    const [currentIndex, setCurrentIndex] = useState(0);
    const [dontShowToday, setDontShowToday] = useState(false);
    const [isVisible, setIsVisible] = useState(false);

    // 우선순위순 정렬
    const sortedNotices = [...notices].sort((a, b) => {
        if (a.priority === "HIGH" && b.priority !== "HIGH") return -1;
        if (a.priority !== "HIGH" && b.priority === "HIGH") return 1;
        return 0;
    });

    const currentNotice = sortedNotices[currentIndex];
    const totalCount = sortedNotices.length;

    useEffect(() => {
        // 오늘 하루 보지 않기 체크
        const hiddenUntil = localStorage.getItem(STORAGE_KEY);
        if (hiddenUntil) {
            const hiddenDate = new Date(hiddenUntil);
            if (hiddenDate > new Date()) {
                setIsVisible(false);
                return;
            }
        }
        setIsVisible(open && notices.length > 0);

        // 팝업이 열릴 때 현재 공지사항을 읽음 처리
        if (open && notices.length > 0 && currentNotice) {
            markNoticeAsRead(currentNotice.id);
        }
    }, [open, notices, currentNotice]);

    // 공지사항 읽음 처리
    const markNoticeAsRead = async (noticeId: number) => {
        try {
            await noticesApi.markAsRead(noticeId);
            console.log(`공지사항 ${noticeId} 읽음 처리 완료`);
        } catch (error) {
            console.error(`공지사항 ${noticeId} 읽음 처리 실패:`, error);
        }
    };

    const handlePrev = () => {
        const newIndex = currentIndex > 0 ? currentIndex - 1 : totalCount - 1;
        setCurrentIndex(newIndex);
        // 새로운 공지사항을 읽음 처리
        markNoticeAsRead(sortedNotices[newIndex].id);
    };

    const handleNext = () => {
        const newIndex = currentIndex < totalCount - 1 ? currentIndex + 1 : 0;
        setCurrentIndex(newIndex);
        // 새로운 공지사항을 읽음 처리
        markNoticeAsRead(sortedNotices[newIndex].id);
    };

    const handleClose = () => {
        if (dontShowToday) {
            // 오늘 자정까지 숨기기
            const tomorrow = new Date();
            tomorrow.setHours(24, 0, 0, 0);
            localStorage.setItem(STORAGE_KEY, tomorrow.toISOString());

            if (onDontShowToday) {
                onDontShowToday(sortedNotices.map((n) => n.id));
            }
        }
        onClose();
    };

    if (!isVisible || sortedNotices.length === 0) {
        return null;
    }

    return (
        <Dialog open={isVisible} onOpenChange={(open) => !open && handleClose()}>
            <DialogContent className="max-w-lg p-0 gap-0 overflow-hidden">
                {/* 헤더 */}
                <DialogHeader className="p-4 pb-2 bg-primary/5">
                    <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2">
                            <Bell className="w-5 h-5 text-primary" />
                            <DialogTitle className="text-lg">공지사항</DialogTitle>
                        </div>
                        <Button
                            variant="ghost"
                            size="icon"
                            className="h-8 w-8"
                            onClick={handleClose}
                        >
                            <X className="w-4 h-4" />
                        </Button>
                    </div>
                    {totalCount > 1 && (
                        <DialogDescription className="text-xs">
                            {currentIndex + 1} / {totalCount}
                        </DialogDescription>
                    )}
                </DialogHeader>

                {/* 공지 내용 */}
                <div className="p-4">
                    <div className="mb-3 flex items-start justify-between gap-2">
                        <div>
                            {currentNotice?.priority === "HIGH" && (
                                <Badge variant="destructive" className="mb-2">
                                    중요
                                </Badge>
                            )}
                            <h3 className="font-semibold text-lg">{currentNotice?.title}</h3>
                            <p className="text-xs text-muted-foreground mt-1">
                                {currentNotice?.createdAt?.split("T")[0]}
                            </p>
                        </div>
                    </div>

                    <div className="prose prose-sm max-w-none text-muted-foreground min-h-[150px] max-h-[300px] overflow-y-auto">
                        <p className="whitespace-pre-line">{currentNotice?.content}</p>
                    </div>
                </div>

                {/* 네비게이션 */}
                {totalCount > 1 && (
                    <div className="flex items-center justify-center gap-2 pb-2">
                        <Button variant="ghost" size="icon" onClick={handlePrev}>
                            <ChevronLeft className="w-4 h-4" />
                        </Button>
                        <div className="flex gap-1">
                            {sortedNotices.map((_, idx) => (
                                <button
                                    key={idx}
                                    className={`w-2 h-2 rounded-full transition-colors ${idx === currentIndex ? "bg-primary" : "bg-muted"
                                        }`}
                                    onClick={() => {
                                        setCurrentIndex(idx);
                                        // 클릭한 공지사항을 읽음 처리
                                        markNoticeAsRead(sortedNotices[idx].id);
                                    }}
                                />
                            ))}
                        </div>
                        <Button variant="ghost" size="icon" onClick={handleNext}>
                            <ChevronRight className="w-4 h-4" />
                        </Button>
                    </div>
                )}

                {/* 푸터 */}
                <div className="p-4 pt-2 border-t bg-muted/30">
                    <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2">
                            <Checkbox
                                id="dontShowToday"
                                checked={dontShowToday}
                                onCheckedChange={(checked) => setDontShowToday(!!checked)}
                            />
                            <Label htmlFor="dontShowToday" className="text-sm cursor-pointer">
                                오늘 하루 보지 않기
                            </Label>
                        </div>
                        <Button onClick={handleClose}>확인</Button>
                    </div>
                </div>
            </DialogContent>
        </Dialog>
    );
};

export default PopupNoticeModal;
