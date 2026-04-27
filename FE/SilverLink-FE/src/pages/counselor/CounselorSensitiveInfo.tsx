import { useState, useEffect } from "react";
import {
  Lock,
  Search,
  Plus,
  Clock,
  CheckCircle2,
  XCircle,
  AlertTriangle,
  Eye,
  Send
} from "lucide-react";
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
  DialogTrigger,
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
import { counselorNavItems } from "@/config/counselorNavItems";
import { useAuth } from "@/contexts/AuthContext";
import assignmentsApi from "@/api/assignments";
import elderlyApi from "@/api/elderly";
import { ElderlySummaryResponse } from "@/types/api";
import accessRequestsApi, { AccessScope } from "@/api/accessRequests";
import { toast } from "sonner";

const infoTypes = [
  { value: "HEALTH_INFO", label: "건강정보" },
  { value: "MEDICATION", label: "복약정보" },
  { value: "CALL_RECORDS", label: "통화기록" },
  { value: "ALL", label: "전체" },
];



const CounselorSensitiveInfo = () => {
  const { user } = useAuth();
  const [searchTerm, setSearchTerm] = useState("");
  const [statusFilter, setStatusFilter] = useState("all");
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [assignedSeniors, setAssignedSeniors] = useState<ElderlySummaryResponse[]>([]);
  const [requests, setRequests] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  const [newRequest, setNewRequest] = useState({
    seniorName: "",
    seniorId: 0,
    guardianName: "",
    infoType: "",
    reason: "",
  });

  const fetchAssignments = async () => {
    try {
      const assignments = await assignmentsApi.getMyAssignments();
      const activeAssignments = assignments.filter(a => a.status === 'ACTIVE');

      const details = await Promise.all(
        activeAssignments.map(a => elderlyApi.getSummary(a.elderlyId))
      );
      setAssignedSeniors(details);
    } catch (error) {
      console.error("Failed to fetch assigned seniors:", error);
    }
  };

  const mapScopeToLabel = (scope: string) => {
    const found = infoTypes.find(t => t.value === scope);
    return found ? found.label : scope;
  };

  const fetchRequests = async () => {
    try {
      setLoading(true);
      // Assuming getMyRequests returns requests made by the current user
      const data = await accessRequestsApi.getMyRequests();

      const mappedRequests = data.map(r => ({
        id: r.id,
        seniorName: r.elderlyName,
        guardianName: r.requesterName,
        infoType: mapScopeToLabel(r.scope),
        reason: "-", // Reason is not available in summary
        status: r.status.toLowerCase(),
        requestDate: r.requestedAt ? r.requestedAt.split('T')[0] : '',
        processDate: r.decidedAt ? r.decidedAt.split('T')[0] : null,
        processedBy: r.reviewedBy,
      }));
      setRequests(mappedRequests);
    } catch (error) {
      console.error("Failed to fetch requests:", error);
      // Fallback or empty state
      setRequests([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRequests();
  }, []);

  const handleOpenDialog = (open: boolean) => {
    setIsDialogOpen(open);
    if (open) {
      fetchAssignments();
    }
  };

  const handleSeniorChange = (name: string) => {
    const selectedSenior = assignedSeniors.find(s => s.name === name);
    setNewRequest({
      ...newRequest,
      seniorName: name,
      seniorId: selectedSenior?.userId || 0,
      guardianName: selectedSenior?.guardianName || ""
    });
  };

  const filteredRequests = requests.filter((req) => {
    const matchesSearch = req.seniorName.includes(searchTerm) || req.guardianName.includes(searchTerm);
    const matchesStatus = statusFilter === "all" || req.status === statusFilter;
    return matchesSearch && matchesStatus;
  });

  const pendingCount = requests.filter(r => r.status === "pending").length;
  const approvedCount = requests.filter(r => r.status === "approved").length;
  const rejectedCount = requests.filter(r => r.status === "rejected").length;

  const getStatusBadge = (status: string) => {
    switch (status) {
      case "approved":
        return <Badge className="bg-success/10 text-success border-0">승인됨</Badge>;
      case "pending":
        return <Badge className="bg-warning/10 text-warning border-0">대기중</Badge>;
      case "rejected":
        return <Badge className="bg-destructive/10 text-destructive border-0">거부됨</Badge>;
      default:
        return <Badge variant="outline">-</Badge>;
    }
  };

  const handleSubmitRequest = async () => {
    if (!newRequest.seniorId || !newRequest.infoType || !newRequest.reason) {
      toast.error("모든 필수 항목을 입력해주세요.");
      return;
    }

    try {
      await accessRequestsApi.createRequest({
        elderlyUserId: newRequest.seniorId,
        scope: newRequest.infoType as AccessScope,
        reason: newRequest.reason
      });

      toast.success("요청이 성공적으로 전송되었습니다.");
      setIsDialogOpen(false);
      setNewRequest({ seniorName: "", seniorId: 0, guardianName: "", infoType: "", reason: "" });
      fetchRequests(); // Refresh list
    } catch (error) {
      console.error("Request failed:", error);
      toast.error("요청 전송에 실패했습니다.");
    }
  };

  return (
    <DashboardLayout
      role="counselor"
      userName={user?.name || "상담사"}
      navItems={counselorNavItems}
    >
      <div className="space-y-6">
        {/* Header */}
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-foreground">민감정보 요청</h1>
            <p className="text-muted-foreground mt-1">어르신 민감정보 열람을 위한 권한을 요청하세요</p>
          </div>
          <Dialog open={isDialogOpen} onOpenChange={handleOpenDialog}>
            <DialogTrigger asChild>
              <Button className="gap-2">
                <Plus className="w-4 h-4" />
                새 요청
              </Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>민감정보 열람 요청</DialogTitle>
              </DialogHeader>
              <div className="space-y-4 mt-4">
                <div className="space-y-2">
                  <Label>어르신 성함</Label>
                  <Select
                    value={newRequest.seniorName}
                    onValueChange={handleSeniorChange}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="어르신을 선택하세요" />
                    </SelectTrigger>
                    <SelectContent>
                      {assignedSeniors.map((senior) => (
                        <SelectItem key={senior.userId} value={senior.name}>
                          {senior.name}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div className="space-y-2">
                  <Label>보호자 성함</Label>
                  <Select
                    value={newRequest.guardianName}
                    onValueChange={(value) => setNewRequest({ ...newRequest, guardianName: value })}
                    disabled={!newRequest.seniorName}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder={newRequest.seniorName ? "보호자를 선택하세요" : "어르신을 먼저 선택하세요"} />
                    </SelectTrigger>
                    <SelectContent>
                      {newRequest.guardianName && (
                        <SelectItem value={newRequest.guardianName}>
                          {newRequest.guardianName}
                        </SelectItem>
                      )}
                      {!newRequest.guardianName && newRequest.seniorName && (
                        <SelectItem value="none" disabled>
                          등록된 보호자가 없습니다
                        </SelectItem>
                      )}
                    </SelectContent>
                  </Select>
                </div>
                <div className="space-y-2">
                  <Label>정보 유형</Label>
                  <Select
                    value={newRequest.infoType}
                    onValueChange={(value) => setNewRequest({ ...newRequest, infoType: value })}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="정보 유형을 선택하세요" />
                    </SelectTrigger>
                    <SelectContent>
                      {infoTypes.map((type) => (
                        <SelectItem key={type.value} value={type.value}>
                          {type.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div className="space-y-2">
                  <Label>요청 사유</Label>
                  <Textarea
                    placeholder="민감정보가 필요한 사유를 상세히 작성해주세요"
                    value={newRequest.reason}
                    onChange={(e) => setNewRequest({ ...newRequest, reason: e.target.value })}
                    rows={4}
                  />
                </div>
              </div>
              <DialogFooter className="mt-4">
                <Button variant="outline" onClick={() => setIsDialogOpen(false)}>
                  취소
                </Button>
                <Button onClick={handleSubmitRequest} className="gap-2">
                  <Send className="w-4 h-4" />
                  요청하기
                </Button>
              </DialogFooter>
            </DialogContent>
          </Dialog>
        </div>

        {/* Alert */}
        <Card className="border-warning/50 bg-warning/5 shadow-card">
          <CardContent className="p-4">
            <div className="flex items-start gap-3">
              <AlertTriangle className="w-5 h-5 text-warning flex-shrink-0 mt-0.5" />
              <div>
                <p className="font-medium text-foreground">민감정보 열람 안내</p>
                <p className="text-sm text-muted-foreground mt-1">
                  민감정보 열람은 어르신의 동의와 관리자의 승인이 필요합니다.
                  요청 사유를 명확히 작성해주시고, 승인된 정보는 업무 목적으로만 사용해주세요.
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
                  <p className="text-sm text-muted-foreground">대기중</p>
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
                  placeholder="어르신 또는 보호자 이름으로 검색..."
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
            <CardDescription>민감정보 열람 요청 현황입니다</CardDescription>
          </CardHeader>
          <CardContent>
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>어르신</TableHead>
                  <TableHead>보호자</TableHead>
                  <TableHead>정보 유형</TableHead>
                  <TableHead>요청 사유</TableHead>
                  <TableHead>요청일</TableHead>
                  <TableHead>상태</TableHead>
                  <TableHead></TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredRequests.map((request) => (
                  <TableRow key={request.id}>
                    <TableCell className="font-medium">{request.seniorName}</TableCell>
                    <TableCell>{request.guardianName}</TableCell>
                    <TableCell>
                      <Badge variant="outline">{request.infoType}</Badge>
                    </TableCell>
                    <TableCell className="max-w-[200px]">
                      <p className="truncate text-sm text-muted-foreground">{request.reason}</p>
                    </TableCell>
                    <TableCell>{request.requestDate}</TableCell>
                    <TableCell>{getStatusBadge(request.status)}</TableCell>
                    <TableCell>
                      {request.status === "approved" && (
                        <Button variant="ghost" size="sm" className="gap-1">
                          <Eye className="w-4 h-4" />
                          열람
                        </Button>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      </div>
    </DashboardLayout>
  );
};

export default CounselorSensitiveInfo;
