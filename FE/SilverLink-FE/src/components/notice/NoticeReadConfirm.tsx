import { useState } from "react";
import { CheckCircle2, Loader2, Users, Eye } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import {
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
} from "@/components/ui/dialog";
import { ScrollArea } from "@/components/ui/scroll-area";
import { useToast } from "@/hooks/use-toast";
import apiClient from "@/api/index";

interface ReadConfirmUser {
    userId: number;
    name: string;
    confirmedAt: string;
}

interface NoticeReadConfirmProps {
    noticeId: number;
    isConfirmed?: boolean;
    confirmCount?: number;
    totalCount?: number;
    onConfirm?: () => void;
    showConfirmList?: boolean;
    disabled?: boolean;
}

/**
 * 공지사항 필독 확인 컴포넌트
 * - "확인했습니다" 버튼
 * - 확인자 목록 조회 (관리자용)
 */
const NoticeReadConfirm = ({
    noticeId,
    isConfirmed = false,
    confirmCount = 0,
    totalCount,
    onConfirm,
    showConfirmList = false,
    disabled = false,
}: NoticeReadConfirmProps) => {
    const { toast } = useToast();
    const [confirmed, setConfirmed] = useState(isConfirmed);
    const [isLoading, setIsLoading] = useState(false);
    const [confirmUsers, setConfirmUsers] = useState<ReadConfirmUser[]>([]);
    const [isListLoading, setIsListLoading] = useState(false);
    const [isDialogOpen, setIsDialogOpen] = useState(false);

    const handleConfirm = async () => {
        if (confirmed || disabled) return;

        try {
            setIsLoading(true);
            await apiClient.post(`/api/notices/${noticeId}/confirm`);
            setConfirmed(true);
            toast({
                title: "확인 완료",
                description: "공지사항을 확인했습니다.",
            });
            if (onConfirm) onConfirm();
        } catch (error) {
            console.error("Failed to confirm notice:", error);
            toast({
                title: "확인 실패",
                description: "다시 시도해주세요.",
                variant: "destructive",
            });
        } finally {
            setIsLoading(false);
        }
    };

    const loadConfirmUsers = async () => {
        try {
            setIsListLoading(true);
            const response = await apiClient.get<ReadConfirmUser[]>(
                `/api/notices/${noticeId}/confirm-list`
            );
            setConfirmUsers(response.data);
        } catch (error) {
            console.error("Failed to load confirm users:", error);
            toast({
                title: "목록 조회 실패",
                description: "확인자 목록을 불러오는데 실패했습니다.",
                variant: "destructive",
            });
        } finally {
            setIsListLoading(false);
        }
    };

    const handleDialogOpen = (open: boolean) => {
        setIsDialogOpen(open);
        if (open) {
            loadConfirmUsers();
        }
    };

    const confirmRate = totalCount ? Math.round((confirmCount / totalCount) * 100) : 0;

    return (
        <div className="space-y-3">
            {/* 확인 버튼 */}
            <Button
                variant={confirmed ? "secondary" : "default"}
                onClick={handleConfirm}
                disabled={confirmed || isLoading || disabled}
                className="w-full"
            >
                {isLoading ? (
                    <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                ) : confirmed ? (
                    <CheckCircle2 className="w-4 h-4 mr-2 text-success" />
                ) : null}
                {confirmed ? "확인완료" : "확인했습니다"}
            </Button>

            {/* 확인 현황 표시 */}
            {(confirmCount > 0 || showConfirmList) && (
                <div className="flex items-center justify-between text-sm">
                    <span className="text-muted-foreground">
                        <Users className="w-4 h-4 inline mr-1" />
                        {confirmCount}명 확인
                        {totalCount && (
                            <span className="ml-1">
                                ({confirmRate}%)
                            </span>
                        )}
                    </span>

                    {/* 확인자 목록 (관리자용) */}
                    {showConfirmList && (
                        <Dialog open={isDialogOpen} onOpenChange={handleDialogOpen}>
                            <DialogTrigger asChild>
                                <Button variant="ghost" size="sm" className="text-primary">
                                    <Eye className="w-4 h-4 mr-1" />
                                    확인자 목록
                                </Button>
                            </DialogTrigger>
                            <DialogContent className="max-w-md">
                                <DialogHeader>
                                    <DialogTitle>확인자 목록</DialogTitle>
                                </DialogHeader>

                                {isListLoading ? (
                                    <div className="flex items-center justify-center py-8">
                                        <Loader2 className="w-6 h-6 animate-spin text-primary" />
                                    </div>
                                ) : confirmUsers.length === 0 ? (
                                    <p className="text-center text-muted-foreground py-8">
                                        아직 확인한 사용자가 없습니다.
                                    </p>
                                ) : (
                                    <ScrollArea className="max-h-[300px]">
                                        <div className="space-y-2">
                                            {confirmUsers.map((user) => (
                                                <div
                                                    key={user.userId}
                                                    className="flex items-center gap-3 p-2 rounded-lg hover:bg-muted/50"
                                                >
                                                    <Avatar className="w-8 h-8">
                                                        <AvatarFallback className="bg-primary/10 text-primary text-sm">
                                                            {user.name?.charAt(0)}
                                                        </AvatarFallback>
                                                    </Avatar>
                                                    <div className="flex-1">
                                                        <p className="text-sm font-medium">{user.name}</p>
                                                        <p className="text-xs text-muted-foreground">
                                                            {new Date(user.confirmedAt).toLocaleString("ko-KR")}
                                                        </p>
                                                    </div>
                                                    <CheckCircle2 className="w-4 h-4 text-success" />
                                                </div>
                                            ))}
                                        </div>
                                    </ScrollArea>
                                )}

                                <div className="text-center text-sm text-muted-foreground border-t pt-3">
                                    총 {confirmUsers.length}명 확인
                                    {totalCount && ` / ${totalCount}명 중`}
                                </div>
                            </DialogContent>
                        </Dialog>
                    )}
                </div>
            )}
        </div>
    );
};

export default NoticeReadConfirm;
