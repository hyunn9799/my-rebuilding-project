import { useState, useEffect } from "react";
import {
  Search,
  Clock,
  CheckCircle2,
  AlertCircle,
  ChevronRight,
  Loader2
} from "lucide-react";
import { adminNavItems } from "@/config/adminNavItems";
import DashboardLayout from "@/components/layout/DashboardLayout";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
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
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { MessageSquare } from "lucide-react";
import { toast } from "sonner";
import complaintsApi, { ComplaintResponse } from "@/api/complaints";
import { useAuth } from "@/contexts/AuthContext";

interface ComplaintDisplay {
  id: number;
  title: string;
  content: string;
  category: string | null;
  status: 'WAITING' | 'PROCESSING' | 'RESOLVED' | 'REJECTED';
  createdAt: string;
  response: string | null;
  respondedByName: string | null;
}

const StatusBadge = ({ status }: { status: string }) => {
  switch (status) {
    case "WAITING":
      return <Badge className="bg-warning/10 text-warning border-0">접수</Badge>;
    case "PROCESSING":
      return <Badge className="bg-info/10 text-info border-0">처리중</Badge>;
    case "RESOLVED":
      return <Badge className="bg-success/10 text-success border-0">완료</Badge>;
    case "REJECTED":
      return <Badge className="bg-destructive/10 text-destructive border-0">반려</Badge>;
    default:
      return <Badge variant="outline">{status}</Badge>;
  }
};

const ComplaintManagement = () => {
  const { user } = useAuth();
  const [complaints, setComplaints] = useState<ComplaintDisplay[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [statusFilter, setStatusFilter] = useState("all");
  const [selectedComplaint, setSelectedComplaint] = useState<ComplaintDisplay | null>(null);
  const [responseText, setResponseText] = useState("");

  // 통계
  const [stats, setStats] = useState({
    total: 0,
    pending: 0,
    inProgress: 0,
    completed: 0,
  });

  // 데이터 로드
  const fetchComplaints = async () => {
    try {
      setLoading(true);

      let response;
      if (statusFilter === "all") {
        response = await complaintsApi.getAllComplaints({ size: 100 });
      } else {
        const status = statusFilter as 'WAITING' | 'PROCESSING' | 'RESOLVED' | 'REJECTED';
        response = await complaintsApi.getComplaintsByStatus(status, { size: 100 });
      }

      const mappedComplaints: ComplaintDisplay[] = (response.content || []).map((c: ComplaintResponse) => ({
        id: c.id,
        title: c.title,
        content: c.content,
        category: c.category,
        status: c.status,
        createdAt: c.createdAt ? new Date(c.createdAt).toLocaleString() : '',
        response: c.response,
        respondedByName: c.respondedByName,
      }));

      setComplaints(mappedComplaints);

      // 통계 계산
      const allResponse = await complaintsApi.getAllComplaints({ size: 1000 });
      const allComplaints = allResponse.content || [];
      setStats({
        total: allComplaints.length,
        pending: allComplaints.filter((c: ComplaintResponse) => c.status === 'WAITING').length,
        inProgress: allComplaints.filter((c: ComplaintResponse) => c.status === 'PROCESSING').length,
        completed: allComplaints.filter((c: ComplaintResponse) => c.status === 'RESOLVED').length,
      });

    } catch (error) {
      console.error("민원 로드 실패:", error);
      toast.error("민원 목록을 불러오는데 실패했습니다.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchComplaints();
  }, [statusFilter]);

  // 검색 필터링
  const filteredComplaints = complaints.filter(c =>
    c.title.includes(searchQuery) || c.content.includes(searchQuery)
  );

  // 상태 변경
  const handleStatusChange = async (newStatus: 'PROCESSING' | 'RESOLVED' | 'REJECTED') => {
    if (!selectedComplaint) return;

    try {
      setSubmitting(true);
      await complaintsApi.updateComplaintStatus(selectedComplaint.id, newStatus);
      toast.success("상태가 변경되었습니다.");
      await fetchComplaints();
      setSelectedComplaint(null);
    } catch (error) {
      console.error("상태 변경 실패:", error);
      toast.error("상태 변경에 실패했습니다.");
    } finally {
      setSubmitting(false);
    }
  };

  // 답변 등록
  const handleReply = async () => {
    if (!selectedComplaint || !responseText.trim()) {
      toast.error("답변 내용을 입력해주세요.");
      return;
    }

    try {
      setSubmitting(true);
      await complaintsApi.replyToComplaint(selectedComplaint.id, responseText);
      toast.success("답변이 등록되었습니다.");
      setResponseText("");
      await fetchComplaints();
      setSelectedComplaint(null);
    } catch (error) {
      console.error("답변 등록 실패:", error);
      toast.error("답변 등록에 실패했습니다.");
    } finally {
      setSubmitting(false);
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
        {/* Page Header */}
        <div>
          <h1 className="text-2xl font-bold text-foreground">불편사항 관리</h1>
          <p className="text-muted-foreground mt-1">보호자 불편사항을 접수하고 처리합니다</p>
        </div>

        {/* Stats Cards */}
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          <Card className="shadow-card border-0">
            <CardContent className="p-4">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center">
                  <MessageSquare className="w-5 h-5 text-primary" />
                </div>
                <div>
                  <p className="text-2xl font-bold">{stats.total}</p>
                  <p className="text-sm text-muted-foreground">전체</p>
                </div>
              </div>
            </CardContent>
          </Card>
          <Card className="shadow-card border-0">
            <CardContent className="p-4">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-warning/10 flex items-center justify-center">
                  <Clock className="w-5 h-5 text-warning" />
                </div>
                <div>
                  <p className="text-2xl font-bold text-warning">{stats.pending}</p>
                  <p className="text-sm text-muted-foreground">접수</p>
                </div>
              </div>
            </CardContent>
          </Card>
          <Card className="shadow-card border-0">
            <CardContent className="p-4">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-info/10 flex items-center justify-center">
                  <AlertCircle className="w-5 h-5 text-info" />
                </div>
                <div>
                  <p className="text-2xl font-bold text-info">{stats.inProgress}</p>
                  <p className="text-sm text-muted-foreground">처리중</p>
                </div>
              </div>
            </CardContent>
          </Card>
          <Card className="shadow-card border-0">
            <CardContent className="p-4">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-success/10 flex items-center justify-center">
                  <CheckCircle2 className="w-5 h-5 text-success" />
                </div>
                <div>
                  <p className="text-2xl font-bold text-success">{stats.completed}</p>
                  <p className="text-sm text-muted-foreground">완료</p>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Filters */}
        <Card className="shadow-card border-0">
          <CardContent className="p-4">
            <div className="flex flex-col md:flex-row gap-4">
              <div className="relative flex-1">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                <Input
                  placeholder="제목, 내용으로 검색..."
                  className="pl-10"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                />
              </div>
              <Select value={statusFilter} onValueChange={setStatusFilter}>
                <SelectTrigger className="w-[150px]">
                  <SelectValue placeholder="상태" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="all">전체 상태</SelectItem>
                  <SelectItem value="WAITING">접수</SelectItem>
                  <SelectItem value="PROCESSING">처리중</SelectItem>
                  <SelectItem value="RESOLVED">완료</SelectItem>
                  <SelectItem value="REJECTED">반려</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </CardContent>
        </Card>

        {/* Complaints List */}
        <Card className="shadow-card border-0">
          <CardContent className="p-0">
            {filteredComplaints.length === 0 ? (
              <div className="p-8 text-center text-muted-foreground">
                민원이 없습니다.
              </div>
            ) : (
              <div className="divide-y divide-border">
                {filteredComplaints.map((complaint) => (
                  <div
                    key={complaint.id}
                    className="p-6 hover:bg-muted/30 transition-colors cursor-pointer"
                    onClick={() => {
                      setSelectedComplaint(complaint);
                      setResponseText("");
                    }}
                  >
                    <div className="flex items-start justify-between gap-4">
                      <div className="flex-1">
                        <div className="flex items-center gap-2 mb-2">
                          <StatusBadge status={complaint.status} />
                          {complaint.category && (
                            <Badge variant="outline">{complaint.category}</Badge>
                          )}
                        </div>
                        <h3 className="font-semibold text-lg mb-1">{complaint.title}</h3>
                        <p className="text-muted-foreground line-clamp-2 mb-3">{complaint.content}</p>
                      </div>
                      <div className="text-right">
                        <p className="text-sm text-muted-foreground">{complaint.createdAt}</p>
                        <Button variant="ghost" size="sm" className="mt-2">
                          상세보기 <ChevronRight className="w-4 h-4 ml-1" />
                        </Button>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>

        {/* Detail Dialog */}
        <Dialog open={!!selectedComplaint} onOpenChange={() => setSelectedComplaint(null)}>
          <DialogContent className="max-w-2xl">
            {selectedComplaint && (
              <>
                <DialogHeader>
                  <div className="flex items-center gap-2 mb-2">
                    <StatusBadge status={selectedComplaint.status} />
                    {selectedComplaint.category && (
                      <Badge variant="outline">{selectedComplaint.category}</Badge>
                    )}
                  </div>
                  <DialogTitle>{selectedComplaint.title}</DialogTitle>
                  <DialogDescription>
                    {selectedComplaint.createdAt}
                  </DialogDescription>
                </DialogHeader>
                <div className="space-y-4 py-4">
                  <div className="p-4 rounded-xl bg-secondary/30">
                    <p className="text-sm text-muted-foreground mb-1">민원 내용</p>
                    <p>{selectedComplaint.content}</p>
                  </div>

                  {selectedComplaint.response && (
                    <div className="p-4 rounded-xl bg-success/10 border border-success/20">
                      <p className="text-sm text-success font-medium mb-1">
                        답변 완료 {selectedComplaint.respondedByName && `(${selectedComplaint.respondedByName})`}
                      </p>
                      <p className="text-foreground">{selectedComplaint.response}</p>
                    </div>
                  )}

                  {selectedComplaint.status !== "RESOLVED" && (
                    <div className="space-y-2">
                      <Label>답변 작성</Label>
                      <Textarea
                        placeholder="민원에 대한 답변을 작성하세요..."
                        value={responseText}
                        onChange={(e) => setResponseText(e.target.value)}
                        rows={4}
                      />
                    </div>
                  )}
                </div>
                <DialogFooter>
                  <Button variant="outline" onClick={() => setSelectedComplaint(null)}>닫기</Button>
                  {selectedComplaint.status !== "RESOLVED" && (
                    <>
                      {selectedComplaint.status === "WAITING" && (
                        <Button
                          variant="secondary"
                          onClick={() => handleStatusChange('PROCESSING')}
                          disabled={submitting}
                        >
                          {submitting && <Loader2 className="w-4 h-4 animate-spin mr-2" />}
                          처리중으로 변경
                        </Button>
                      )}
                      <Button onClick={handleReply} disabled={submitting || !responseText.trim()}>
                        {submitting && <Loader2 className="w-4 h-4 animate-spin mr-2" />}
                        답변 등록
                      </Button>
                    </>
                  )}
                </DialogFooter>
              </>
            )}
          </DialogContent>
        </Dialog>
      </div>
    </DashboardLayout>
  );
};

export default ComplaintManagement;
