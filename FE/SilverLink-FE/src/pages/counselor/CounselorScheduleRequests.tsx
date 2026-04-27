import { useState, useEffect } from "react";
import DashboardLayout from "@/components/layout/DashboardLayout";
import { counselorNavItems } from "@/config/counselorNavItems";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import {
    Dialog,
    DialogContent,
    DialogDescription,
    DialogFooter,
    DialogHeader,
    DialogTitle,
} from "@/components/ui/dialog";
import {
    Clock,
    Calendar,
    User,
    CheckCircle,
    XCircle,
    Loader2,
    RefreshCw,
    ClipboardList,
} from "lucide-react";
import { useAuth } from "@/contexts/AuthContext";
import { toast } from "sonner";
import {
    getPendingRequests,
    approveScheduleRequest,
    rejectScheduleRequest,
    ScheduleChangeRequest,
} from "@/api/callSchedules";

const DAY_LABELS: Record<string, string> = {
    MON: "월",
    TUE: "화",
    WED: "수",
    THU: "목",
    FRI: "금",
};

const CounselorScheduleRequests = () => {
    const { user } = useAuth();
    const [loading, setLoading] = useState(true);
    const [requests, setRequests] = useState<ScheduleChangeRequest[]>([]);
    const [selectedRequest, setSelectedRequest] = useState<ScheduleChangeRequest | null>(null);
    const [rejectDialogOpen, setRejectDialogOpen] = useState(false);
    const [rejectReason, setRejectReason] = useState("");
    const [processing, setProcessing] = useState(false);

    useEffect(() => {
        loadRequests();
    }, []);

    const loadRequests = async () => {
        setLoading(true);
        try {
            const data = await getPendingRequests();
            setRequests(data);
        } catch (error) {
            console.error("Failed to load requests:", error);
            toast.error("요청 목록을 불러오는데 실패했습니다.");
        } finally {
            setLoading(false);
        }
    };

    const handleApprove = async (request: ScheduleChangeRequest) => {
        setProcessing(true);
        try {
            await approveScheduleRequest(request.id);
            toast.success(`${request.elderlyName}님의 스케줄 변경 요청을 승인했습니다.`);
            loadRequests();
        } catch (error: any) {
            const message = error.response?.data?.message || "승인에 실패했습니다.";
            toast.error(message);
        } finally {
            setProcessing(false);
        }
    };

    const handleReject = async () => {
        if (!selectedRequest) return;

        setProcessing(true);
        try {
            await rejectScheduleRequest(selectedRequest.id, rejectReason);
            toast.success(`${selectedRequest.elderlyName}님의 스케줄 변경 요청을 거절했습니다.`);
            setRejectDialogOpen(false);
            setRejectReason("");
            setSelectedRequest(null);
            loadRequests();
        } catch (error: any) {
            const message = error.response?.data?.message || "거절에 실패했습니다.";
            toast.error(message);
        } finally {
            setProcessing(false);
        }
    };

    const openRejectDialog = (request: ScheduleChangeRequest) => {
        setSelectedRequest(request);
        setRejectDialogOpen(true);
    };

    return (
        <DashboardLayout
            role="counselor"
            userName={user?.name || "상담사"}
            navItems={counselorNavItems}
        >
            <div className="space-y-6">
                {/* Header */}
                <div className="flex items-center justify-between">
                    <div>
                        <h1 className="text-2xl font-bold text-foreground flex items-center gap-2">
                            <ClipboardList className="w-7 h-7" />
                            스케줄 변경 요청
                        </h1>
                        <p className="text-muted-foreground mt-1">
                            어르신들의 통화 스케줄 변경 요청을 관리합니다
                        </p>
                    </div>
                    <Button variant="outline" onClick={loadRequests} disabled={loading}>
                        <RefreshCw className={`w-4 h-4 mr-2 ${loading ? "animate-spin" : ""}`} />
                        새로고침
                    </Button>
                </div>

                {/* Stats */}
                <Card>
                    <CardContent className="p-6">
                        <div className="flex items-center gap-4">
                            <div className="w-14 h-14 rounded-xl bg-orange-100 flex items-center justify-center">
                                <Clock className="w-7 h-7 text-orange-600" />
                            </div>
                            <div>
                                <p className="text-3xl font-bold">{requests.length}</p>
                                <p className="text-muted-foreground">대기 중인 요청</p>
                            </div>
                        </div>
                    </CardContent>
                </Card>

                {/* Request List */}
                {loading ? (
                    <div className="flex items-center justify-center py-16">
                        <Loader2 className="w-10 h-10 animate-spin text-primary" />
                    </div>
                ) : requests.length === 0 ? (
                    <Card>
                        <CardContent className="p-12 text-center">
                            <CheckCircle className="w-12 h-12 text-green-500 mx-auto mb-4" />
                            <p className="text-lg font-medium">모든 요청을 처리했습니다</p>
                            <p className="text-muted-foreground mt-2">
                                현재 대기 중인 스케줄 변경 요청이 없습니다
                            </p>
                        </CardContent>
                    </Card>
                ) : (
                    <div className="grid gap-4">
                        {requests.map((request) => (
                            <Card key={request.id} className="hover:shadow-md transition-shadow">
                                <CardContent className="p-6">
                                    <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4">
                                        {/* Request Info */}
                                        <div className="space-y-3 flex-1">
                                            <div className="flex items-center gap-3">
                                                <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center">
                                                    <User className="w-5 h-5 text-primary" />
                                                </div>
                                                <div>
                                                    <p className="font-semibold text-lg">{request.elderlyName}</p>
                                                    <p className="text-sm text-muted-foreground">
                                                        요청일: {new Date(request.createdAt).toLocaleDateString("ko-KR", {
                                                            year: "numeric",
                                                            month: "long",
                                                            day: "numeric",
                                                            hour: "2-digit",
                                                            minute: "2-digit",
                                                        })}
                                                    </p>
                                                </div>
                                            </div>

                                            <div className="flex flex-wrap items-center gap-4 pl-13">
                                                <div className="flex items-center gap-2">
                                                    <Clock className="w-4 h-4 text-blue-600" />
                                                    <span className="font-medium">{request.requestedCallTime}</span>
                                                </div>
                                                <div className="flex items-center gap-2">
                                                    <Calendar className="w-4 h-4 text-green-600" />
                                                    <div className="flex gap-1">
                                                        {request.requestedCallDays.map((day) => (
                                                            <Badge key={day} variant="secondary">
                                                                {DAY_LABELS[day] || day}
                                                            </Badge>
                                                        ))}
                                                    </div>
                                                </div>
                                            </div>
                                        </div>

                                        {/* Actions */}
                                        <div className="flex gap-2 lg:flex-col xl:flex-row">
                                            <Button
                                                onClick={() => handleApprove(request)}
                                                disabled={processing}
                                                className="flex-1 lg:w-24"
                                            >
                                                <CheckCircle className="w-4 h-4 mr-1" />
                                                승인
                                            </Button>
                                            <Button
                                                variant="destructive"
                                                onClick={() => openRejectDialog(request)}
                                                disabled={processing}
                                                className="flex-1 lg:w-24"
                                            >
                                                <XCircle className="w-4 h-4 mr-1" />
                                                거절
                                            </Button>
                                        </div>
                                    </div>
                                </CardContent>
                            </Card>
                        ))}
                    </div>
                )}
            </div>

            {/* Reject Dialog */}
            <Dialog open={rejectDialogOpen} onOpenChange={setRejectDialogOpen}>
                <DialogContent>
                    <DialogHeader>
                        <DialogTitle>변경 요청 거절</DialogTitle>
                        <DialogDescription>
                            {selectedRequest?.elderlyName}님의 스케줄 변경 요청을 거절합니다.
                            거절 사유를 입력해주세요 (선택사항).
                        </DialogDescription>
                    </DialogHeader>

                    <div className="py-4">
                        <Textarea
                            placeholder="거절 사유를 입력하세요..."
                            value={rejectReason}
                            onChange={(e) => setRejectReason(e.target.value)}
                            rows={3}
                        />
                    </div>

                    <DialogFooter>
                        <Button
                            variant="outline"
                            onClick={() => {
                                setRejectDialogOpen(false);
                                setRejectReason("");
                                setSelectedRequest(null);
                            }}
                        >
                            취소
                        </Button>
                        <Button
                            variant="destructive"
                            onClick={handleReject}
                            disabled={processing}
                        >
                            {processing ? <Loader2 className="w-4 h-4 animate-spin mr-2" /> : null}
                            거절하기
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>
        </DashboardLayout>
    );
};

export default CounselorScheduleRequests;
