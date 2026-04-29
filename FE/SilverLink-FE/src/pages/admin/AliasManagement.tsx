import { useCallback, useEffect, useState } from "react";
import {
  AlertTriangle,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Clock,
  Hash,
  Loader2,
  Pill,
  RefreshCw,
  XCircle,
} from "lucide-react";
import { toast } from "sonner";

import aliasAdminApi, {
  type AliasSuggestionItem,
  type AliasSuggestionPageResponse,
} from "@/api/aliasAdmin";
import DashboardLayout from "@/components/layout/DashboardLayout";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { adminNavItems } from "@/config/adminNavItems";
import { useAuth } from "@/contexts/AuthContext";

const PAGE_SIZE = 20;

const statusConfig: Record<string, { label: string; color: string }> = {
  PENDING: { label: "대기", color: "bg-amber-100 text-amber-700 border-amber-200" },
  APPROVED: { label: "승인됨", color: "bg-green-100 text-green-700 border-green-200" },
  REJECTED: { label: "거부됨", color: "bg-red-100 text-red-700 border-red-200" },
};

const typeConfig: Record<string, { label: string; color: string }> = {
  alias: { label: "별칭", color: "bg-blue-100 text-blue-700 border-blue-200" },
  error_alias: { label: "OCR 오류 별칭", color: "bg-orange-100 text-orange-700 border-orange-200" },
};

const getErrorMessage = (error: unknown, fallback: string) => {
  if (typeof error === "object" && error !== null && "response" in error) {
    const response = (error as { response?: { data?: { message?: string; detail?: string } } }).response;
    return response?.data?.message || response?.data?.detail || fallback;
  }
  if (error instanceof Error) return error.message;
  return fallback;
};

const AliasManagement = () => {
  const { user } = useAuth();
  const [data, setData] = useState<AliasSuggestionPageResponse>({
    items: [],
    total: 0,
    page: 1,
    size: PAGE_SIZE,
  });
  const [isLoading, setIsLoading] = useState(false);
  const [actionLoadingId, setActionLoadingId] = useState<number | null>(null);
  const [filterStatus, setFilterStatus] = useState("PENDING");
  const [isReloading, setIsReloading] = useState(false);

  const fetchData = useCallback(
    async (page = 1) => {
      setIsLoading(true);
      try {
        const result = await aliasAdminApi.getAliasSuggestions(page, PAGE_SIZE, filterStatus);
        setData(result);
      } catch (error) {
        toast.error(getErrorMessage(error, "Alias 제안 목록을 불러오지 못했습니다."));
      } finally {
        setIsLoading(false);
      }
    },
    [filterStatus],
  );

  useEffect(() => {
    fetchData(1);
  }, [fetchData]);

  const reviewerName = user?.name || "admin";

  const handleApprove = async (item: AliasSuggestionItem) => {
    setActionLoadingId(item.id);
    try {
      const result = await aliasAdminApi.approveSuggestion(item.id, reviewerName);
      if (result.success) {
        if (result.reload_success === false) {
          toast.warning(result.reload_warning || "승인은 완료됐지만 사전 리로드에 실패했습니다.");
        } else {
          toast.success(result.message || "Alias 제안을 승인했습니다.");
        }
        fetchData(data.page);
      } else {
        toast.error(result.message || "Alias 제안 승인에 실패했습니다.");
      }
    } catch (error) {
      toast.error(getErrorMessage(error, "Alias 제안 승인에 실패했습니다."));
    } finally {
      setActionLoadingId(null);
    }
  };

  const handleReject = async (item: AliasSuggestionItem) => {
    setActionLoadingId(item.id);
    try {
      const result = await aliasAdminApi.rejectSuggestion(item.id, reviewerName);
      if (result.success) {
        toast.success(result.message || "Alias 제안을 거부했습니다.");
        fetchData(data.page);
      } else {
        toast.error(result.message || "Alias 제안 거부에 실패했습니다.");
      }
    } catch (error) {
      toast.error(getErrorMessage(error, "Alias 제안 거부에 실패했습니다."));
    } finally {
      setActionLoadingId(null);
    }
  };

  const handleReloadDictionary = async () => {
    setIsReloading(true);
    try {
      const result = await aliasAdminApi.reloadDictionary();
      if (result.success) {
        toast.success(result.message || "의약품 사전을 다시 불러왔습니다.");
      } else {
        toast.error(result.message || "의약품 사전 리로드에 실패했습니다.");
      }
    } catch (error) {
      toast.error(getErrorMessage(error, "의약품 사전 리로드에 실패했습니다."));
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
    <DashboardLayout role="admin" userName={user?.name || "관리자"} navItems={adminNavItems}>
      <div className="space-y-6">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="text-2xl font-bold text-foreground">Alias 제안 관리</h1>
            <p className="mt-1 text-muted-foreground">
              OCR 사용자 피드백으로 생성된 의약품 별칭 제안을 검토합니다.
            </p>
          </div>
          <div className="flex gap-2">
            <Button
              variant="outline"
              onClick={() => fetchData(data.page)}
              disabled={isLoading}
              className="gap-2"
            >
              <RefreshCw className={`h-4 w-4 ${isLoading ? "animate-spin" : ""}`} />
              새로고침
            </Button>
            <Button
              onClick={handleReloadDictionary}
              disabled={isReloading}
              className="gap-2"
              variant="secondary"
            >
              {isReloading ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <RefreshCw className="h-4 w-4" />
              )}
              사전 리로드
            </Button>
          </div>
        </div>

        <Card className="shadow-card border-0">
          <CardContent className="p-4">
            <div className="flex flex-col gap-4 sm:flex-row sm:items-center">
              <div className="flex flex-1 items-center gap-2">
                <span className="whitespace-nowrap text-sm font-medium text-muted-foreground">
                  상태 필터
                </span>
                <Select value={filterStatus} onValueChange={setFilterStatus}>
                  <SelectTrigger className="w-40">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="PENDING">대기</SelectItem>
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

        {isLoading ? (
          <Card className="shadow-card border-0">
            <CardContent className="p-12 text-center">
              <Loader2 className="mx-auto h-8 w-8 animate-spin text-muted-foreground" />
              <p className="mt-3 text-muted-foreground">불러오는 중입니다.</p>
            </CardContent>
          </Card>
        ) : data.items.length === 0 ? (
          <Card className="shadow-card border-0">
            <CardContent className="p-12 text-center">
              <Pill className="mx-auto mb-4 h-12 w-12 text-muted-foreground" />
              <p className="text-muted-foreground">
                {filterStatus === "PENDING"
                  ? "대기 중인 Alias 제안이 없습니다."
                  : "표시할 Alias 제안이 없습니다."}
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
                  className="shadow-card border-0 transition-shadow hover:shadow-elevated"
                >
                  <CardContent className="p-4 sm:p-5">
                    <div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-start">
                      <div className="flex-1 space-y-2">
                        <div className="flex flex-wrap items-center gap-2">
                          <span className="text-base font-bold text-foreground">
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
                            <Hash className="h-3 w-3" />
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
                            <Clock className="h-3 w-3" />
                            {formatDate(item.created_at)}
                          </span>
                          {item.reviewed_by && (
                            <span>
                              검토자 {item.reviewed_by} ({formatDate(item.reviewed_at)})
                            </span>
                          )}
                          {item.alias_normalized && item.alias_normalized !== item.alias_name && (
                            <span>정규화 "{item.alias_normalized}"</span>
                          )}
                        </div>
                      </div>

                      {item.review_status === "PENDING" && (
                        <div className="flex gap-2 self-end sm:self-start">
                          <Button
                            size="sm"
                            onClick={() => handleApprove(item)}
                            disabled={isActionLoading}
                            className="gap-1 bg-green-600 hover:bg-green-700"
                          >
                            {isActionLoading ? (
                              <Loader2 className="h-4 w-4 animate-spin" />
                            ) : (
                              <CheckCircle2 className="h-4 w-4" />
                            )}
                            승인
                          </Button>
                          <Button
                            size="sm"
                            variant="outline"
                            onClick={() => handleReject(item)}
                            disabled={isActionLoading}
                            className="gap-1 border-red-200 text-red-600 hover:bg-red-50"
                          >
                            {isActionLoading ? (
                              <Loader2 className="h-4 w-4 animate-spin" />
                            ) : (
                              <XCircle className="h-4 w-4" />
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

        {data.total > data.size && (
          <div className="flex items-center justify-center gap-4">
            <Button
              variant="outline"
              size="sm"
              disabled={data.page <= 1 || isLoading}
              onClick={() => fetchData(data.page - 1)}
            >
              <ChevronLeft className="h-4 w-4" />
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
              <ChevronRight className="h-4 w-4" />
            </Button>
          </div>
        )}

        <Card className="shadow-card border-amber-200 bg-amber-50/50">
          <CardHeader className="pb-2">
            <CardTitle className="flex items-center gap-2 text-base">
              <AlertTriangle className="h-4 w-4 text-amber-600" />
              운영 안내
            </CardTitle>
          </CardHeader>
          <CardContent className="space-y-1 text-sm text-muted-foreground">
            <p>
              <strong>별칭(alias)</strong>은 정식 의약품명의 다른 표현입니다.
            </p>
            <p>
              <strong>OCR 오류 별칭(error_alias)</strong>은 OCR이 반복적으로 잘못 읽는 표현입니다.
            </p>
            <p>승인하면 실제 매칭 사전에 추가되어 다음 OCR부터 적용됩니다.</p>
            <p>승인 후 사전 리로드가 성공하면 AI 서버의 인메모리 인덱스가 즉시 갱신됩니다.</p>
          </CardContent>
        </Card>
      </div>
    </DashboardLayout>
  );
};

export default AliasManagement;
