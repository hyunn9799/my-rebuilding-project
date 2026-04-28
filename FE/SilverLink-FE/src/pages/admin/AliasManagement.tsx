import { useState, useEffect, useCallback } from "react";
import DashboardLayout from "@/components/layout/DashboardLayout";
import { adminNavItems } from "@/config/adminNavItems";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import {
  CheckCircle2,
  XCircle,
  Loader2,
  RefreshCw,
  Pill,
  AlertTriangle,
  ChevronLeft,
  ChevronRight,
  Hash,
  Clock,
} from "lucide-react";
import { toast } from "sonner";
import { useAuth } from "@/contexts/AuthContext";
import aliasAdminApi, {
  type AliasSuggestionItem,
  type AliasSuggestionPageResponse,
} from "@/api/aliasAdmin";

const statusConfig: Record<string, { label: string; color: string }> = {
  PENDING: { label: "대기", color: "bg-amber-100 text-amber-700 border-amber-200" },
  APPROVED: { label: "승인됨", color: "bg-green-100 text-green-700 border-green-200" },
  REJECTED: { label: "거부됨", color: "bg-red-100 text-red-700 border-red-200" },
};

const typeConfig: Record<string, { label: string; color: string }> = {
  alias: { label: "별칭", color: "bg-blue-100 text-blue-700 border-blue-200" },
  error_alias: { label: "오류 변형", color: "bg-orange-100 text-orange-700 border-orange-200" },
};

const AliasManagement = () => {
  const { user } = useAuth();

  const [data, setData] = useState<AliasSuggestionPageResponse>({
    items: [],
    total: 0,
    page: 1,
    size: 20,
  });
  const [isLoading, setIsLoading] = useState(false);
  const [actionLoadingId, setActionLoadingId] = useState<number | null>(null);
  const [filterStatus, setFilterStatus] = useState("PENDING");
  const [isReloading, setIsReloading] = useState(false);

  const fetchData = useCallback(async (page: number = 1) => {
    setIsLoading(true);
    try {
      const result = await aliasAdminApi.getAliasSuggestions(
        page,
        20,
        filterStatus,
      );
      setData(result);
    } catch (error: any) {
      const msg = error?.response?.data?.message || error?.message || "데이터 조회 실패";
      toast.error(msg);
    } finally {
      setIsLoading(false);
    }
  }, [filterStatus]);

  useEffect(() => {
    fetchData(1);
  }, [fetchData]);

  const handleApprove = async (item: AliasSuggestionItem) => {
    setActionLoadingId(item.id);
    try {
      const result = await aliasAdminApi.approveSuggestion(
        item.id,
        user?.name || "admin",
      );
      if (result.success) {
        if (result.reload_success === false) {
            toast.warning(result.reload_warning || "사전 리로드 실패. 서버에서 확인해주세요.");
        } else {
            toast.success(result.message);
        }
        fetchData(data.page);
      } else {
        toast.error(result.message);
      }
    } catch (error: any) {
      const msg = error?.response?.data?.message || error?.message || "승인 실패";
      toast.error(msg);
    } finally {
      setActionLoadingId(null);
    }
  };

  const handleReject = async (item: AliasSuggestionItem) => {
    setActionLoadingId(item.id);
    try {
      const result = await aliasAdminApi.rejectSuggestion(
        item.id,
        user?.name || "admin",
      );
      if (result.success) {
        toast.success(result.message);
        fetchData(data.page);
      } else {
        toast.error(result.message);
      }
    } catch (error: any) {
      const msg = error?.response?.data?.message || error?.message || "거부 실패";
      toast.error(msg);
    } finally {
      setActionLoadingId(null);
    }
  };

  const handleReloadDictionary = async () => {
    setIsReloading(true);
    try {
      const result = await aliasAdminApi.reloadDictionary();
      if (result.success) {
        toast.success(result.message);
      } else {
        toast.error(result.message);
      }
    } catch (error: any) {
      const msg = error?.response?.data?.message || error?.message || "리로드 실패";
      toast.error(msg);
    } finally {
      setIsReloading(false);
    }
  };

  const totalPages = Math.ceil(data.total / data.size) || 1;

  const formatDate = (dateStr?: string) => {
    if (!dateStr) return "-";
    try {
      return new Date(dateStr).toLocaleString("ko-KR", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
      });
    } catch {
      return dateStr;
    }
  };

  return (
    <DashboardLayout
      role="admin"
      userName={user?.name || "관리자"}
      navItems={adminNavItems}
    >
      <div className="space-y-6">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-foreground">Alias 제안 관리</h1>
            <p className="text-muted-foreground mt-1">
              OCR 사용자 피드백 기반 약품 별칭 제안을 검토합니다
            </p>
          </div>
          <div className="flex gap-2">
            <Button
              variant="outline"
              onClick={() => fetchData(data.page)}
              disabled={isLoading}
              className="gap-2"
            >
              <RefreshCw className={`w-4 h-4 ${isLoading ? "animate-spin" : ""}`} />
              새로고침
            </Button>
            <Button
              onClick={handleReloadDictionary}
              disabled={isReloading}
              className="gap-2"
              variant="secondary"
            >
              {isReloading ? (
                <Loader2 className="w-4 h-4 animate-spin" />
              ) : (
                <RefreshCw className="w-4 h-4" />
              )}
              사전 리로드
            </Button>
          </div>
        </div>

        {/* Filter */}
        <Card className="shadow-card border-0">
          <CardContent className="p-4">
            <div className="flex flex-col sm:flex-row gap-4 items-center">
              <div className="flex items-center gap-2 flex-1">
                <span className="text-sm font-medium text-muted-foreground whitespace-nowrap">
                  상태 필터:
                </span>
                <Select value={filterStatus} onValueChange={setFilterStatus}>
                  <SelectTrigger className="w-40">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="PENDING">대기 (PENDING)</SelectItem>
                    <SelectItem value="APPROVED">승인됨</SelectItem>
                    <SelectItem value="REJECTED">거부됨</SelectItem>
                    <SelectItem value="ALL">전체</SelectItem>
                  </SelectContent>
                </Select>
              </div>
              <div className="text-sm text-muted-foreground">
                총 <span className="font-bold text-foreground">{data.total}</span>건
              </div>
            </div>
          </CardContent>
        </Card>

        {/* List */}
        {isLoading ? (
          <Card className="shadow-card border-0">
            <CardContent className="p-12 text-center">
              <Loader2 className="w-8 h-8 mx-auto text-muted-foreground animate-spin" />
              <p className="text-muted-foreground mt-3">로딩 중...</p>
            </CardContent>
          </Card>
        ) : data.items.length === 0 ? (
          <Card className="shadow-card border-0">
            <CardContent className="p-12 text-center">
              <Pill className="w-12 h-12 text-muted-foreground mx-auto mb-4" />
              <p className="text-muted-foreground">
                {filterStatus === "PENDING"
                  ? "대기 중인 제안이 없습니다"
                  : "표시할 제안이 없습니다"}
              </p>
            </CardContent>
          </Card>
        ) : (
          <div className="space-y-3">
            {data.items.map((item) => {
              const status = statusConfig[item.review_status] || statusConfig.PENDING;
              const type = typeConfig[item.suggestion_type] || typeConfig.error_alias;
              const isActionLoading = actionLoadingId === item.id;

              return (
                <Card
                  key={item.id}
                  className="shadow-card border-0 hover:shadow-elevated transition-shadow"
                >
                  <CardContent className="p-4 sm:p-5">
                    <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4">
                      {/* Left: Info */}
                      <div className="flex-1 space-y-2">
                        <div className="flex flex-wrap items-center gap-2">
                          <span className="font-bold text-base text-foreground">
                            "{item.alias_name}"
                          </span>
                          <span className="text-muted-foreground">→</span>
                          <span className="font-semibold text-info">
                            {item.item_name || item.item_seq}
                          </span>
                        </div>

                        <div className="flex flex-wrap gap-2">
                          <Badge variant="outline" className={status.color}>
                            {status.label}
                          </Badge>
                          <Badge variant="outline" className={type.color}>
                            {type.label}
                          </Badge>
                          <Badge variant="outline" className="gap-1">
                            <Hash className="w-3 h-3" />
                            {item.frequency}회
                          </Badge>
                          {item.source && (
                            <Badge variant="outline" className="text-xs">
                              {item.source}
                            </Badge>
                          )}
                        </div>

                        <div className="flex flex-wrap gap-3 text-xs text-muted-foreground">
                          <span className="flex items-center gap-1">
                            <Clock className="w-3 h-3" />
                            {formatDate(item.created_at)}
                          </span>
                          {item.reviewed_by && (
                            <span>검토: {item.reviewed_by} ({formatDate(item.reviewed_at)})</span>
                          )}
                          {item.alias_normalized && item.alias_normalized !== item.alias_name && (
                            <span>정규화: "{item.alias_normalized}"</span>
                          )}
                        </div>
                      </div>

                      {/* Right: Actions */}
                      {item.review_status === "PENDING" && (
                        <div className="flex gap-2 self-end sm:self-start">
                          <Button
                            size="sm"
                            onClick={() => handleApprove(item)}
                            disabled={isActionLoading}
                            className="gap-1 bg-green-600 hover:bg-green-700"
                          >
                            {isActionLoading ? (
                              <Loader2 className="w-4 h-4 animate-spin" />
                            ) : (
                              <CheckCircle2 className="w-4 h-4" />
                            )}
                            승인
                          </Button>
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => handleReject(item)}
                            disabled={isActionLoading}
                            className="gap-1 text-red-600 border-red-200 hover:bg-red-50"
                          >
                            {isActionLoading ? (
                              <Loader2 className="w-4 h-4 animate-spin" />
                            ) : (
                              <XCircle className="w-4 h-4" />
                            )}
                            거부
                          </Button>
                        </div>
                      )}
                    </div>
                  </CardContent>
                </Card>
              );
            })}
          </div>
        )}

        {/* Pagination */}
        {data.total > data.size && (
          <div className="flex justify-center items-center gap-4">
            <Button
              variant="outline"
              size="sm"
              disabled={data.page <= 1 || isLoading}
              onClick={() => fetchData(data.page - 1)}
            >
              <ChevronLeft className="w-4 h-4" />
            </Button>
            <span className="text-sm text-muted-foreground">
              {data.page} / {totalPages}
            </span>
            <Button
              variant="outline"
              size="sm"
              disabled={data.page >= totalPages || isLoading}
              onClick={() => fetchData(data.page + 1)}
            >
              <ChevronRight className="w-4 h-4" />
            </Button>
          </div>
        )}

        {/* Info Card */}
        <Card className="shadow-card border-0 bg-amber-50/50 border-amber-200">
          <CardHeader className="pb-2">
            <CardTitle className="text-base flex items-center gap-2">
              <AlertTriangle className="w-4 h-4 text-amber-600" />
              운영 안내
            </CardTitle>
          </CardHeader>
          <CardContent className="text-sm text-muted-foreground space-y-1">
            <p>• <strong>별칭(alias)</strong>: 정식 약품명의 다른 표현 (예: "타이레놀" → 타이레놀정500밀리그램)</p>
            <p>• <strong>오류 변형(error_alias)</strong>: OCR 오인식 패턴 (예: "타이레놜" → 타이레놀정)</p>
            <p>• 승인하면 해당 별칭이 실제 매칭 사전에 추가되어 다음 OCR부터 적용됩니다.</p>
            <p>• 승인 후 <strong>사전 리로드</strong>를 누르면 AI 서버의 인메모리 인덱스가 즉시 갱신됩니다.</p>
          </CardContent>
        </Card>
      </div>
    </DashboardLayout>
  );
};

export default AliasManagement;
