import { useState } from "react";
import { Bell, X, ChevronRight } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { useNavigate } from "react-router-dom";

export interface UnreadNotice {
    id: number;
    title: string;
    isPriority: boolean;
}

interface UnreadNoticeAlertProps {
    notices: UnreadNotice[];
    onClose: () => void;
    noticesPath: string; // 공지사항 페이지 경로
}

const STORAGE_KEY = "hidden_popup_notices";

/**
 * 읽지 않은 팝업 공지사항 알림
 * - 화면 상단에 작은 배너 형태로 표시
 * - 제목만 간단히 표시
 * - 클릭 시 공지사항 페이지로 이동
 * - 팝업 공지로 설정된 공지사항만 표시
 * - "오늘 하루 보지 않기" 기능 제공
 */
const UnreadNoticeAlert = ({
    notices,
    onClose,
    noticesPath,
}: UnreadNoticeAlertProps) => {
    const navigate = useNavigate();
    const [isVisible, setIsVisible] = useState(true);

    const handleClose = () => {
        setIsVisible(false);
        onClose();
    };

    const handleDontShowToday = () => {
        // 오늘 하루 보지 않기 설정
        const today = new Date().toDateString();
        const noticeIds = notices.map(n => n.id);
        
        localStorage.setItem(STORAGE_KEY, JSON.stringify({
            date: today,
            noticeIds: noticeIds
        }));
        
        console.log("오늘 하루 보지 않기 설정:", noticeIds);
        
        // 알림 닫기
        setIsVisible(false);
        onClose();
    };

    const handleClick = () => {
        // 공지사항 페이지로 이동
        navigate(noticesPath);
    };

    if (!isVisible || notices.length === 0) {
        return null;
    }

    const priorityCount = notices.filter(n => n.isPriority).length;
    const firstNotice = notices[0];

    return (
        <div className="fixed top-4 left-1/2 -translate-x-1/2 z-50 w-full max-w-2xl px-4 animate-in slide-in-from-top duration-300">
            <div className="bg-primary text-primary-foreground rounded-lg shadow-lg border border-primary/20">
                <div className="flex items-center gap-3 p-3">
                    {/* 아이콘 */}
                    <div className="flex-shrink-0">
                        <div className="w-10 h-10 rounded-full bg-primary-foreground/20 flex items-center justify-center">
                            <Bell className="w-5 h-5" />
                        </div>
                    </div>

                    {/* 내용 */}
                    <button
                        onClick={handleClick}
                        className="flex-1 text-left hover:opacity-80 transition-opacity"
                    >
                        <div className="flex items-center gap-2 mb-1">
                            <p className="font-semibold text-sm">
                                새로운 팝업 공지사항이 있습니다
                            </p>
                            {priorityCount > 0 && (
                                <Badge variant="destructive" className="text-xs">
                                    중요 {priorityCount}개
                                </Badge>
                            )}
                        </div>
                        <p className="text-xs opacity-90 line-clamp-1">
                            {notices.length === 1 
                                ? firstNotice.title
                                : `${firstNotice.title} 외 ${notices.length - 1}개`
                            }
                        </p>
                    </button>

                    {/* 버튼들 */}
                    <div className="flex items-center gap-2 flex-shrink-0">
                        <Button
                            variant="ghost"
                            size="sm"
                            onClick={handleDontShowToday}
                            className="text-primary-foreground hover:bg-primary-foreground/20 h-8 px-3"
                        >
                            <span className="text-xs">오늘 그만 보기</span>
                        </Button>
                        <Button
                            variant="ghost"
                            size="sm"
                            onClick={handleClick}
                            className="text-primary-foreground hover:bg-primary-foreground/20 h-8 px-3"
                        >
                            <span className="text-xs">확인</span>
                            <ChevronRight className="w-4 h-4 ml-1" />
                        </Button>
                        <Button
                            variant="ghost"
                            size="icon"
                            onClick={handleClose}
                            className="text-primary-foreground hover:bg-primary-foreground/20 h-8 w-8"
                        >
                            <X className="w-4 h-4" />
                        </Button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default UnreadNoticeAlert;
