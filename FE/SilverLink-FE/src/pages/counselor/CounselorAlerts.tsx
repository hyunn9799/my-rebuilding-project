import { useState, useEffect, useCallback } from "react";
import {
  AlertTriangle,
  CheckCircle2,
  Clock,
  PhoneCall,
  Loader2,
  RefreshCw,
  X
} from "lucide-react";
import DashboardLayout from "@/components/layout/DashboardLayout";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import { counselorNavItems } from "@/config/counselorNavItems";
import { useAuth } from "@/contexts/AuthContext";
import { toast } from "sonner";
import {
  getAlertsForCounselor,
  getStatsForCounselor,
  getAlertDetail,
  processAlert,
  startProcessing,
  EmergencyAlertSummary,
  EmergencyAlertStats,
  EmergencyAlertDetail,
  Severity,
  AlertStatus
} from "@/api/emergencyAlerts";

const SeverityBadge = ({ severity }: { severity: Severity }) => {
  switch (severity) {
    case "CRITICAL":
      return <Badge variant="destructive" className="animate-pulse">긴급</Badge>;
    case "HIGH":
      return <Badge className="bg-warning text-warning-foreground">주의</Badge>;
    case "MEDIUM":
      return <Badge variant="outline">보통</Badge>;
    case "LOW":
      return <Badge variant="secondary">낮음</Badge>;
    default:
      return <Badge variant="secondary">{severity}</Badge>;
  }
};

const StatusBadge = ({ status }: { status: AlertStatus }) => {
  switch (status) {
    case "PENDING":
      return <Badge variant="outline" className="bg-muted">대기</Badge>;
    case "IN_PROGRESS":
      return <Badge className="bg-info text-info-foreground">처리중</Badge>;
    case "RESOLVED":
      return <Badge className="bg-success/10 text-success border-0">처리완료</Badge>;
    case "ESCALATED":
      return <Badge className="bg-warning/10 text-warning border-0">상위보고</Badge>;
    default:
      return <Badge variant="secondary">{status}</Badge>;
  }
};

const CounselorAlerts = () => {
  const { user } = useAuth();
  const [alerts, setAlerts] = useState<EmergencyAlertSummary[]>([]);
  const [stats, setStats] = useState<EmergencyAlertStats | null>(null);
  const [loading, setLoading] = useState(true);
  const [selectedAlert, setSelectedAlert] = useState<EmergencyAlertDetail | null>(null);
  const [showDetailDialog, setShowDetailDialog] = useState(false);
  const [showProcessDialog, setShowProcessDialog] = useState(false);
  const [processingAlertId, setProcessingAlertId] = useState<number | null>(null);
  const [resolutionNote, setResolutionNote] = useState("");
  const [isProcessing, setIsProcessing] = useState(false);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  // 알림 목록 및 통계 조회
  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      const [alertsResponse, statsResponse] = await Promise.all([
        getAlertsForCounselor({ page, size: 20 }),
        getStatsForCounselor()
      ]);
      setAlerts(alertsResponse.content);
      setTotalPages(alertsResponse.totalPages);
      setStats(statsResponse);
    } catch (error) {
      console.error('Failed to fetch alerts:', error);
      toast.error('알림 목록을 불러오는데 실패했습니다.');
    } finally {
      setLoading(false);
    }
  }, [page]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  // 알림 상세 조회
  const handleViewDetail = async (alertId: number) => {
    try {
      const detail = await getAlertDetail(alertId);
      setSelectedAlert(detail);
      setShowDetailDialog(true);
    } catch (error) {
      console.error('Failed to fetch alert detail:', error);
      toast.error('알림 상세 정보를 불러오는데 실패했습니다.');
    }
  };

  // 처리 시작
  const handleStartProcessing = async (alertId: number) => {
    try {
      setIsProcessing(true);
      await startProcessing(alertId);
      toast.success('알림 처리를 시작했습니다.');
      fetchData();
    } catch (error) {
      console.error('Failed to start processing:', error);
      toast.error('처리 시작에 실패했습니다.');
    } finally {
      setIsProcessing(false);
    }
  };

  // 처리 완료 다이얼로그 열기
  const handleOpenProcessDialog = (alertId: number) => {
    setProcessingAlertId(alertId);
    setResolutionNote("");
    setShowProcessDialog(true);
  };

  // 처리 완료
  const handleProcessComplete = async () => {
    if (!processingAlertId) return;

    try {
      setIsProcessing(true);
      await processAlert(processingAlertId, {
        status: 'RESOLVED',
        resolutionNote: resolutionNote || undefined
      });
      toast.success('알림이 처리 완료되었습니다.');
      setShowProcessDialog(false);
      setProcessingAlertId(null);
      setResolutionNote("");
      fetchData();
    } catch (error) {
      console.error('Failed to process alert:', error);
      toast.error('처리 완료에 실패했습니다.');
    } finally {
      setIsProcessing(false);
    }
  };

  // 상위 보고
  const handleEscalate = async () => {
    if (!processingAlertId) return;

    try {
      setIsProcessing(true);
      await processAlert(processingAlertId, {
        status: 'ESCALATED',
        resolutionNote: resolutionNote || undefined
      });
      toast.success('관리자에게 상위 보고되었습니다.');
      setShowProcessDialog(false);
      setProcessingAlertId(null);
      setResolutionNote("");
      fetchData();
    } catch (error) {
      console.error('Failed to escalate alert:', error);
      toast.error('상위 보고에 실패했습니다.');
    } finally {
      setIsProcessing(false);
    }
  };

  const pendingCount = stats?.pendingCount ?? 0;
  const criticalCount = stats?.criticalCount ?? 0;
  const highCount = stats?.highCount ?? 0;
  const resolvedCount = stats?.resolvedCount ?? 0;

  return (
    <DashboardLayout
      role="counselor"
      userName={user?.name || "상담사"}
      navItems={counselorNavItems}
    >
      {/* Alert Detail Dialog */}
      <Dialog open={showDetailDialog} onOpenChange={setShowDetailDialog}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              {selectedAlert && <SeverityBadge severity={selectedAlert.severity} />}
              {selectedAlert?.title}
            </DialogTitle>
            <DialogDescription>{selectedAlert?.alertTypeText}</DialogDescription>
          </DialogHeader>
          {selectedAlert && (
            <div className="space-y-4">
              <div className="p-4 rounded-xl bg-muted">
                <p className="text-foreground">{selectedAlert.description}</p>
              </div>

              {/* 어르신 정보 */}
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <h4 className="font-semibold mb-2">어르신 정보</h4>
                  <div className="text-sm space-y-1">
                    <p>이름: {selectedAlert.elderly.name} ({selectedAlert.elderly.age}세)</p>
                    <p>연락처: {selectedAlert.elderly.phone}</p>
                    <p>주소: {selectedAlert.elderly.address}</p>
                  </div>
                </div>
                {selectedAlert.guardian && (
                  <div>
                    <h4 className="font-semibold mb-2">보호자 정보</h4>
                    <div className="text-sm space-y-1">
                      <p>이름: {selectedAlert.guardian.name}</p>
                      <p>관계: {selectedAlert.guardian.relation}</p>
                      <p>연락처: {selectedAlert.guardian.phone}</p>
                    </div>
                  </div>
                )}
              </div>

              {/* 위험 키워드 */}
              {selectedAlert.dangerKeywords && selectedAlert.dangerKeywords.length > 0 && (
                <div>
                  <h4 className="font-semibold mb-2">감지된 위험 키워드</h4>
                  <div className="flex flex-wrap gap-2">
                    {selectedAlert.dangerKeywords.map((keyword, idx) => (
                      <Badge key={idx} variant="destructive">{keyword}</Badge>
                    ))}
                  </div>
                </div>
              )}

              {/* 처리 정보 */}
              {selectedAlert.process && (
                <div className="p-4 rounded-xl bg-success/10">
                  <h4 className="font-semibold mb-2">처리 정보</h4>
                  <div className="text-sm space-y-1">
                    <p>처리자: {selectedAlert.process.processedByName}</p>
                    <p>처리일시: {selectedAlert.process.processedAt}</p>
                    {selectedAlert.process.resolutionNote && (
                      <p>처리 메모: {selectedAlert.process.resolutionNote}</p>
                    )}
                  </div>
                </div>
              )}
            </div>
          )}
          <DialogFooter className="gap-2">
            {selectedAlert?.elderly.phone && (
              <Button variant="outline" asChild>
                <a href={`tel:${selectedAlert.elderly.phone}`}>
                  <PhoneCall className="w-4 h-4 mr-2" />
                  어르신 연락
                </a>
              </Button>
            )}
            {selectedAlert?.guardian?.phone && (
              <Button variant="outline" asChild>
                <a href={`tel:${selectedAlert.guardian.phone}`}>
                  <PhoneCall className="w-4 h-4 mr-2" />
                  보호자 연락
                </a>
              </Button>
            )}
            <Button variant="secondary" onClick={() => setShowDetailDialog(false)}>
              닫기
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Process Dialog */}
      <Dialog open={showProcessDialog} onOpenChange={setShowProcessDialog}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>알림 처리</DialogTitle>
            <DialogDescription>처리 결과를 입력해주세요.</DialogDescription>
          </DialogHeader>
          <div className="space-y-4">
            <div>
              <Label htmlFor="resolutionNote">처리 메모 (선택)</Label>
              <Textarea
                id="resolutionNote"
                value={resolutionNote}
                onChange={(e) => setResolutionNote(e.target.value)}
                placeholder="조치 내용을 입력하세요..."
                className="mt-2"
                rows={4}
              />
            </div>
          </div>
          <DialogFooter className="gap-2">
            <Button variant="outline" onClick={() => setShowProcessDialog(false)} disabled={isProcessing}>
              취소
            </Button>
            <Button variant="secondary" onClick={handleEscalate} disabled={isProcessing}>
              {isProcessing ? <Loader2 className="w-4 h-4 mr-2 animate-spin" /> : null}
              상위 보고
            </Button>
            <Button onClick={handleProcessComplete} disabled={isProcessing}>
              {isProcessing ? <Loader2 className="w-4 h-4 mr-2 animate-spin" /> : <CheckCircle2 className="w-4 h-4 mr-2" />}
              처리 완료
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <div className="space-y-6">
        {/* Page Header */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-foreground">긴급 알림</h1>
            <p className="text-muted-foreground mt-1">어르신 위험 상황을 모니터링합니다</p>
          </div>
          <div className="flex gap-2">
            <Button variant="outline" onClick={fetchData} disabled={loading}>
              <RefreshCw className={`w-4 h-4 mr-2 ${loading ? 'animate-spin' : ''}`} />
              새로고침
            </Button>
            {pendingCount > 0 && (
              <Button variant="destructive">
                <AlertTriangle className="w-4 h-4 mr-2" />
                미처리 {pendingCount}건
              </Button>
            )}
          </div>
        </div>

        {/* Stats */}
        <div className="grid grid-cols-4 gap-4">
          <Card className="shadow-card border-0 border-l-4 border-l-destructive">
            <CardContent className="p-4">
              <p className="text-3xl font-bold text-destructive">{criticalCount}</p>
              <p className="text-sm text-muted-foreground">긴급</p>
            </CardContent>
          </Card>
          <Card className="shadow-card border-0 border-l-4 border-l-warning">
            <CardContent className="p-4">
              <p className="text-3xl font-bold text-warning">{highCount}</p>
              <p className="text-sm text-muted-foreground">주의</p>
            </CardContent>
          </Card>
          <Card className="shadow-card border-0 border-l-4 border-l-info">
            <CardContent className="p-4">
              <p className="text-3xl font-bold text-info">{stats?.inProgressCount ?? 0}</p>
              <p className="text-sm text-muted-foreground">처리중</p>
            </CardContent>
          </Card>
          <Card className="shadow-card border-0 border-l-4 border-l-success">
            <CardContent className="p-4">
              <p className="text-3xl font-bold text-success">{resolvedCount}</p>
              <p className="text-sm text-muted-foreground">처리 완료</p>
            </CardContent>
          </Card>
        </div>

        {/* Alerts List */}
        <Card className="shadow-card border-0">
          <CardHeader>
            <CardTitle className="text-lg">알림 목록</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            {loading ? (
              <div className="flex items-center justify-center py-12">
                <Loader2 className="w-8 h-8 animate-spin text-muted-foreground" />
              </div>
            ) : alerts.length === 0 ? (
              <div className="text-center py-12 text-muted-foreground">
                <AlertTriangle className="w-12 h-12 mx-auto mb-4 opacity-50" />
                <p>등록된 긴급 알림이 없습니다.</p>
              </div>
            ) : (
              alerts.map((alert) => (
                <div
                  key={alert.alertId}
                  className={`p-5 rounded-xl border-2 cursor-pointer transition-colors hover:bg-muted/50 ${alert.severity === "CRITICAL" ? "border-destructive bg-destructive/5" :
                      alert.severity === "HIGH" ? "border-warning bg-warning/5" :
                        "border-border bg-muted/30"
                    } ${alert.status === "RESOLVED" || alert.status === "ESCALATED" ? "opacity-60" : ""}`}
                  onClick={() => handleViewDetail(alert.alertId)}
                >
                  <div className="flex items-start justify-between gap-4">
                    <div className="flex items-start gap-4">
                      <Avatar className="w-12 h-12">
                        <AvatarFallback className={`${alert.severity === "CRITICAL" ? "bg-destructive text-destructive-foreground" :
                            alert.severity === "HIGH" ? "bg-warning text-warning-foreground" :
                              "bg-secondary"
                          }`}>
                          {alert.elderlyName.charAt(0)}
                        </AvatarFallback>
                      </Avatar>
                      <div>
                        <div className="flex items-center gap-2 mb-1 flex-wrap">
                          <span className="font-semibold text-lg">{alert.elderlyName}</span>
                          <span className="text-muted-foreground">({alert.elderlyAge}세)</span>
                          <SeverityBadge severity={alert.severity} />
                          <Badge variant="outline">{alert.alertTypeText}</Badge>
                          <StatusBadge status={alert.status} />
                        </div>
                        <p className="text-foreground mb-2">{alert.description}</p>
                        {alert.guardianName && (
                          <p className="text-sm text-muted-foreground">
                            보호자: {alert.guardianName} ({alert.guardianPhone})
                          </p>
                        )}
                      </div>
                    </div>
                    <div className="text-right shrink-0">
                      <p className="text-sm text-muted-foreground mb-3">{alert.timeAgo}</p>
                      {alert.status === "PENDING" && (
                        <div className="flex gap-2" onClick={(e) => e.stopPropagation()}>
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => handleStartProcessing(alert.alertId)}
                            disabled={isProcessing}
                          >
                            <Clock className="w-3 h-3 mr-1" />
                            처리시작
                          </Button>
                          <Button
                            size="sm"
                            onClick={() => handleOpenProcessDialog(alert.alertId)}
                          >
                            <CheckCircle2 className="w-3 h-3 mr-1" />
                            처리완료
                          </Button>
                        </div>
                      )}
                      {alert.status === "IN_PROGRESS" && (
                        <div onClick={(e) => e.stopPropagation()}>
                          <Button
                            size="sm"
                            onClick={() => handleOpenProcessDialog(alert.alertId)}
                          >
                            <CheckCircle2 className="w-3 h-3 mr-1" />
                            처리완료
                          </Button>
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              ))
            )}

            {/* Pagination */}
            {totalPages > 1 && (
              <div className="flex justify-center gap-2 pt-4">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={page === 0}
                  onClick={() => setPage(p => Math.max(0, p - 1))}
                >
                  이전
                </Button>
                <span className="flex items-center px-4 text-sm text-muted-foreground">
                  {page + 1} / {totalPages}
                </span>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={page >= totalPages - 1}
                  onClick={() => setPage(p => p + 1)}
                >
                  다음
                </Button>
              </div>
            )}
          </CardContent>
        </Card>
      </div>
    </DashboardLayout>
  );
};

export default CounselorAlerts;
