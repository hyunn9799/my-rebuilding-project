import { useState, useEffect } from "react";
import {
  Search,
  CheckCircle2,
  XCircle,
  Clock,
  Eye,
  ShieldCheck,
  Loader2
} from "lucide-react";
import { adminNavItems } from "@/config/adminNavItems";
import DashboardLayout from "@/components/layout/DashboardLayout";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Lock } from "lucide-react";
import { toast } from "sonner";
import accessRequestsApi, { AccessRequestResponse, AccessRequestStatus } from "@/api/accessRequests";
import { useAuth } from "@/contexts/AuthContext";

interface RequestDisplay {
  id: number;
  guardianName: string;
  elderlyName: string;
  scope: string;
  reason: string;
  status: AccessRequestStatus;
  createdAt: string;
  processedAt?: string;
  processedByName?: string;
  rejectReason?: string;
  documentsVerified: boolean;
}

const SensitiveInfoManagement = () => {
  const { user } = useAuth();
  const [requests, setRequests] = useState<RequestDisplay[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState("all");
  const [selectedRequest, setSelectedRequest] = useState<RequestDisplay | null>(null);
  const [rejectReason, setRejectReason] = useState("");

  // 통계
  const [stats, setStats] = useState({
    total: 0,
    pending: 0,
    approved: 0,
    rejected: 0,
  });

  // 데이터 로드
  const fetchRequests = async () => {
    try {
      setLoading(true);
      const pendingData = await accessRequestsApi.getPendingRequests();

      // 응답을 화면에 표시할 형태로 변환
      // map from AccessRequestSummary
      const mappedRequests: RequestDisplay[] = pendingData.map((r: any) => ({
        id: r.id,
        guardianName: r.requesterName || '보호자',
        elderlyName: r.elderlyName || '어르신',
        scope: mapScopeToKorean(r.scope),
        reason: "-", // Summary does not include request reason
        status: r.status,
        createdAt: r.requestedAt ? new Date(r.requestedAt).toLocaleDateString() : '',
        processedAt: r.decidedAt ? new Date(r.decidedAt).toLocaleDateString() : undefined,
        processedByName: r.reviewedBy,
        rejectReason: r.decisionNote,
        documentsVerified: r.documentsVerified,
      }));

      setRequests(mappedRequests);

      // 통계 업데이트
      const statsData = await accessRequestsApi.getPendingStats();
      setStats({
        total: statsData.total,
        pending: statsData.pending,
        approved: 0, // 별도 API 필요
        rejected: 0, // 별도 API 필요
      });

    } catch (error) {
      console.error("요청 목록 로드 실패:", error);
      toast.error("요청 목록을 불러오는데 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRequests();
  }, []);

  const mapErrorMessage = (message: string): string => {
    const errorMap: Record<string, string> = {
      'DOCUMENT_NOT_VERIFIED': '서류 확인이 완료되지 않았습니다.',
      'REQUEST_NOT_FOUND': '해당 요청을 찾을 수 없습니다.',
      'REQUEST_ALREADY_PROCESSED': '이미 처리된 요청입니다.',
      'INVALID_REQUEST_STATUS': '유효하지 않은 요청 상태입니다.',
    };
    return errorMap[message] || message;
  };

  const mapScopeToKorean = (scope: string): string => {
    switch (scope) {
      case 'HEALTH_INFO': return '건강정보';
      case 'MEDICATION': return '복약정보';
      case 'CALL_RECORDS': return '통화기록';
      case 'ALL': return '전체';
      default: return scope;
    }
  };

  const filteredRequests = requests.filter((req) => {
    const matchesSearch =
      req.guardianName.includes(searchTerm) ||
      req.elderlyName.includes(searchTerm);
    const matchesStatus = statusFilter === "all" || req.status === statusFilter.toUpperCase();
    return matchesSearch && matchesStatus;
  });

  const pendingCount = requests.filter(r => r.status === "PENDING" || r.status === "DOCUMENTS_VERIFIED").length;
  const approvedCount = requests.filter(r => r.status === "APPROVED").length;
  const rejectedCount = requests.filter(r => r.status === "REJECTED").length;

  const getStatusBadge = (status: string) => {
    switch (status) {
      case "APPROVED":
        return <Badge className="bg-success/10 text-success border-0">승인됨</Badge>;
      case "PENDING":
        return <Badge className="bg-warning/10 text-warning border-0">대기중</Badge>;
      case "DOCUMENTS_VERIFIED":
        return <Badge className="bg-info/10 text-info border-0">서류확인</Badge>;
      case "REJECTED":
        return <Badge className="bg-destructive/10 text-destructive border-0">거부됨</Badge>;
      case "REVOKED":
        return <Badge className="bg-muted text-muted-foreground border-0">철회됨</Badge>;
      default:
        return <Badge variant="outline">-</Badge>;
    }
  };

  const handleApprove = async () => {
    if (!selectedRequest) return;

    try {
      setSubmitting(true);

      // 30일 후 만료 계산
      const expiresAt = new Date();
      expiresAt.setDate(expiresAt.getDate() + 30);

      // LocalDateTime 형식에 맞체 'Z' 제거 (YYYY-MM-DDTHH:mm:ss)
      const formattedDate = expiresAt.toISOString().slice(0, 19);

      await accessRequestsApi.approveRequest(selectedRequest.id, {
        accessRequestId: selectedRequest.id,
        expiresAt: formattedDate,
        note: "승인됨"
      });

      toast.success("요청이 승인되었습니다.");
      await fetchRequests();
      setSelectedRequest(null);
    } catch (error: any) {
      console.error("승인 실패:", error);
      const rawMessage = error.response?.data?.message || "요청 승인에 실패했습니다.";
      toast.error(mapErrorMessage(rawMessage));
    } finally {
      setSubmitting(false);
    }
  };

  const handleReject = async () => {
    if (!selectedRequest) return;

    if (!rejectReason.trim()) {
      toast.error("거부 사유를 입력해주세요.");
      return;
    }

    if (rejectReason.trim().length < 10) {
      toast.error("거부 사유는 최소 10자 이상 입력해야 합니다.");
      return;
    }

    try {
      setSubmitting(true);
      await accessRequestsApi.rejectRequest(selectedRequest.id, {
        accessRequestId: selectedRequest.id,
        reason: rejectReason
      });
      toast.success("요청이 거부되었습니다.");
      await fetchRequests();
      setSelectedRequest(null);
      setRejectReason("");
    } catch (error: any) {
      console.error("거부 실패:", error);
      const rawMessage = error.response?.data?.message || "요청 거부에 실패했습니다.";
      toast.error(mapErrorMessage(rawMessage));
    } finally {
      setSubmitting(false);
    }
  };

  const [verifyConfirmId, setVerifyConfirmId] = useState<number | null>(null);

  const handleVerifyDocuments = async () => {
    if (!verifyConfirmId) return;
    try {
      await accessRequestsApi.verifyDocuments(verifyConfirmId);
      toast.success("서류 확인이 완료되었습니다.");
      setVerifyConfirmId(null);
      await fetchRequests();
    } catch (error) {
      console.error("서류 확인 실패:", error);
      toast.error("서류 확인에 실패했습니다.");
    }
  };

  if (loading) {
    return (
      <DashboardLayout role="admin" userName={user?.name || "관리자"} navItems={adminNavItems}>
        <div className="flex items-center justify-center h-64">
          <Loader2 className="w-8 h-8 animate-spin text-primary" />
        </div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout
      role="admin"
      userName={user?.name || "관리자"}
      navItems={adminNavItems}
    >
      <div className="space-y-6">
        {/* Header */}
        <div>
          <h1 className="text-2xl font-bold text-foreground">민감정보 요청 관리</h1>
          <p className="text-muted-foreground mt-1">보호자들의 민감정보 열람 요청을 승인하거나 거부하세요</p>
        </div>

        {/* Alert */}
        <Card className="border-info/50 bg-info/5 shadow-card">
          <CardContent className="p-4">
            <div className="flex items-start gap-3">
              <ShieldCheck className="w-5 h-5 text-info flex-shrink-0 mt-0.5" />
              <div>
                <p className="font-medium text-foreground">민감정보 관리 가이드</p>
                <p className="text-sm text-muted-foreground mt-1">
                  민감정보 열람 승인 시 반드시 어르신의 동의 여부를 확인하세요.
                  승인된 정보는 업무 목적으로만 사용되어야 하며, 모든 열람 기록은 시스템에 저장됩니다.
                </p>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Stats */}
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          <Card className="shadow-card border-0">
            <CardContent className="p-4">
              <div className="flex items-center gap-3">
                <div className="p-2 rounded-full bg-primary/10">
                  <Lock className="h-5 w-5 text-primary" />
                </div>
                <div>
                  <p className="text-2xl font-bold">{requests.length}</p>
                  <p className="text-sm text-muted-foreground">전체 요청</p>
                </div>
              </div>
            </CardContent>
          </Card>
          <Card className="shadow-card border-0">
            <CardContent className="p-4">
              <div className="flex items-center gap-3">
                <div className="p-2 rounded-full bg-warning/10">
                  <Clock className="h-5 w-5 text-warning" />
                </div>
                <div>
                  <p className="text-2xl font-bold">{pendingCount}</p>
                  <p className="text-sm text-muted-foreground">처리 대기</p>
                </div>
              </div>
            </CardContent>
          </Card>
          <Card className="shadow-card border-0">
            <CardContent className="p-4">
              <div className="flex items-center gap-3">
                <div className="p-2 rounded-full bg-success/10">
                  <CheckCircle2 className="h-5 w-5 text-success" />
                </div>
                <div>
                  <p className="text-2xl font-bold">{approvedCount}</p>
                  <p className="text-sm text-muted-foreground">승인됨</p>
                </div>
              </div>
            </CardContent>
          </Card>
          <Card className="shadow-card border-0">
            <CardContent className="p-4">
              <div className="flex items-center gap-3">
                <div className="p-2 rounded-full bg-destructive/10">
                  <XCircle className="h-5 w-5 text-destructive" />
                </div>
                <div>
                  <p className="text-2xl font-bold">{rejectedCount}</p>
                  <p className="text-sm text-muted-foreground">거부됨</p>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Filters */}
        <Card className="shadow-card border-0">
          <CardContent className="pt-6">
            <div className="flex flex-col gap-4 md:flex-row md:items-center">
              <div className="relative flex-1">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input
                  placeholder="보호자, 어르신 이름으로 검색..."
                  className="pl-9"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                />
              </div>
              <Select value={statusFilter} onValueChange={setStatusFilter}>
                <SelectTrigger className="w-[140px]">
                  <SelectValue placeholder="상태" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">전체</SelectItem>
                  <SelectItem value="pending">대기중</SelectItem>
                  <SelectItem value="approved">승인됨</SelectItem>
                  <SelectItem value="rejected">거부됨</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </CardContent>
        </Card>

        {/* Requests Table */}
        <Card className="shadow-card border-0">
          <CardHeader>
            <CardTitle>요청 목록</CardTitle>
            <CardDescription>대기 중인 요청을 우선 처리해주세요 ({filteredRequests.length}건)</CardDescription>
          </CardHeader>
          <CardContent>
            {filteredRequests.length === 0 ? (
              <div className="text-center py-8 text-muted-foreground">
                요청 내역이 없습니다.
              </div>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>보호자</TableHead>
                    <TableHead>어르신</TableHead>
                    <TableHead>정보 유형</TableHead>
                    <TableHead>서류 확인</TableHead>
                    <TableHead>요청일</TableHead>
                    <TableHead>상태</TableHead>
                    <TableHead></TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {filteredRequests.map((request) => (
                    <TableRow key={request.id}>
                      <TableCell className="font-medium">{request.guardianName}</TableCell>
                      <TableCell>{request.elderlyName}</TableCell>
                      <TableCell>
                        <Badge variant="outline">{request.scope}</Badge>
                      </TableCell>
                      <TableCell>
                        {request.documentsVerified ? (
                          <Badge className="bg-success/10 text-success border-0">확인완료</Badge>
                        ) : (
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => setVerifyConfirmId(request.id)}
                          >
                            서류확인
                          </Button>
                        )}
                      </TableCell>
                      <TableCell>{request.createdAt}</TableCell>
                      <TableCell>{getStatusBadge(request.status)}</TableCell>
                      <TableCell>
                        {(request.status === "PENDING" || request.status === "DOCUMENTS_VERIFIED") ? (
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => setSelectedRequest(request)}
                          >
                            처리하기
                          </Button>
                        ) : (
                          <Button variant="ghost" size="sm" className="gap-1">
                            <Eye className="w-4 h-4" />
                            상세
                          </Button>
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Verify Documents Confirmation Dialog */}
      <Dialog open={!!verifyConfirmId} onOpenChange={() => setVerifyConfirmId(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>서류 확인</DialogTitle>
          </DialogHeader>
          <p className="text-sm text-muted-foreground">
            해당 요청의 서류 확인을 완료하시겠습니까? 확인 후에는 되돌릴 수 없습니다.
          </p>
          <DialogFooter className="mt-4">
            <Button variant="outline" onClick={() => setVerifyConfirmId(null)}>
              취소
            </Button>
            <Button onClick={handleVerifyDocuments} className="gap-1">
              <CheckCircle2 className="w-4 h-4" />
              확인 완료
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Process Dialog */}
      <Dialog open={!!selectedRequest} onOpenChange={() => setSelectedRequest(null)}>
        <DialogContent className="max-w-lg">
          <DialogHeader>
            <DialogTitle>민감정보 요청 처리</DialogTitle>
          </DialogHeader>

          <div className="space-y-4 mt-4">
            <div className="grid grid-cols-2 gap-4 p-4 bg-secondary/30 rounded-xl">
              <div>
                <p className="text-sm text-muted-foreground">요청 보호자</p>
                <p className="font-medium">{selectedRequest?.guardianName}</p>
              </div>
              <div>
                <p className="text-sm text-muted-foreground">대상 어르신</p>
                <p className="font-medium">{selectedRequest?.elderlyName}</p>
              </div>
              <div>
                <p className="text-sm text-muted-foreground">정보 유형</p>
                <p className="font-medium">{selectedRequest?.scope}</p>
              </div>
              <div>
                <p className="text-sm text-muted-foreground">서류 확인</p>
                <p className="font-medium">
                  {selectedRequest?.documentsVerified ? '확인 완료' : '미확인'}
                </p>
              </div>
            </div>

            <div>
              <p className="text-sm text-muted-foreground mb-2">요청 사유</p>
              <p className="p-4 bg-secondary/30 rounded-xl text-sm">
                {selectedRequest?.reason}
              </p>
            </div>

            {selectedRequest?.documentsVerified && (
              <div className="flex items-center gap-2 p-4 rounded-xl bg-success/10">
                <CheckCircle2 className="w-5 h-5 text-success" />
                <span className="text-sm">서류 확인 완료됨</span>
              </div>
            )}

            <div className="space-y-2">
              <Label>거부 시 사유 (거부할 경우 필수)</Label>
              <Textarea
                placeholder="거부 사유를 입력하세요..."
                value={rejectReason}
                onChange={(e) => setRejectReason(e.target.value)}
                rows={3}
              />
            </div>
          </div>

          <DialogFooter className="mt-4 gap-2">
            <Button variant="outline" onClick={() => setSelectedRequest(null)}>
              취소
            </Button>
            <Button
              variant="destructive"
              onClick={handleReject}
              disabled={submitting}
              className="gap-1"
            >
              {submitting && <Loader2 className="w-4 h-4 animate-spin" />}
              <XCircle className="w-4 h-4" />
              거부
            </Button>
            <Button
              onClick={handleApprove}
              disabled={submitting}
              className="gap-1"
            >
              {submitting && <Loader2 className="w-4 h-4 animate-spin" />}
              <CheckCircle2 className="w-4 h-4" />
              승인
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </DashboardLayout>
  );
};

export default SensitiveInfoManagement;
