import { useState, useEffect } from "react";
import DashboardLayout from "@/components/layout/DashboardLayout";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
    Table,
    TableBody,
    TableCell,
    TableHead,
    TableHeader,
    TableRow,
} from "@/components/ui/table";
import { RefreshCw, Search } from "lucide-react";
import { adminNavItems } from "@/config/adminNavItems";
import { useAuth } from "@/contexts/AuthContext";
import welfareApi from "@/api/welfare";
import { WelfareListResponse } from "@/types/api";
import { toast } from "sonner";
import { Badge } from "@/components/ui/badge";

export default function WelfareServiceManagement() {
    const { user } = useAuth();
    const [welfareList, setWelfareList] = useState<WelfareListResponse[]>([]);
    const [loading, setLoading] = useState(false);
    const [syncing, setSyncing] = useState(false);
    const [searchTerm, setSearchTerm] = useState("");
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);

    const fetchWelfareList = async () => {
        setLoading(true);
        try {
            const response = await welfareApi.searchWelfare({
                page: page,
                size: 10,
                keyword: searchTerm,
            });
            setWelfareList(response.content);
            setTotalPages(response.totalPages);
        } catch (error) {
            console.error("Failed to fetch welfare list:", error);
            toast.error("복지 서비스 목록을 불러오는데 실패했습니다.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchWelfareList();
    }, [page, searchTerm]);

    const handleSync = async () => {
        if (!confirm("데이터 동기화를 시작하시겠습니까? 데이터 양에 따라 시간이 소요될 수 있습니다.")) {
            return;
        }

        setSyncing(true);
        try {
            const message = await welfareApi.manualSync();
            toast.success(message);
            // 잠시 후 목록 갱신 (비동기 작업이라 즉시 반영은 안될 수 있음)
            setTimeout(fetchWelfareList, 3000);
        } catch (error) {
            console.error("Sync failed:", error);
            toast.error("데이터 동기화 요청 실패");
        } finally {
            setSyncing(false);
        }
    };

    const getSourceBadge = (source: string) => {
        if (source === "CENTRAL") return <Badge variant="default">중앙</Badge>;
        if (source === "LOCAL") return <Badge variant="outline">지자체</Badge>;
        return <Badge variant="secondary">{source}</Badge>;
    };

    return (
        <DashboardLayout
            role="admin"
            userName={user?.name || "관리자"}
            userAvatar="/placeholder.svg"
            navItems={adminNavItems}
        >
            <div className="space-y-6">
                <div className="flex justify-between items-center">
                    <div>
                        <h1 className="text-2xl font-bold">복지 서비스 관리</h1>
                        <p className="text-muted-foreground">
                            공공데이터 포털의 노인 복지 정보를 조회하고 동기화합니다.
                        </p>
                    </div>
                    <Button onClick={handleSync} disabled={syncing}>
                        <RefreshCw className={`mr-2 h-4 w-4 ${syncing ? "animate-spin" : ""}`} />
                        {syncing ? "동기화 중..." : "데이터 수동 동기화"}
                    </Button>
                </div>

                <Card>
                    <CardHeader>
                        <div className="flex items-center gap-4">
                            <div className="relative flex-1 max-w-sm">
                                <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
                                <Input
                                    className="pl-8"
                                    placeholder="서비스명 검색..."
                                    value={searchTerm}
                                    onChange={(e) => {
                                        setSearchTerm(e.target.value);
                                        setPage(0); // 검색어 변경 시 첫 페이지로
                                    }}
                                />
                            </div>
                        </div>
                    </CardHeader>
                    <CardContent>
                        <Table>
                            <TableHeader>
                                <TableRow>
                                    <TableHead className="w-[100px]">ID</TableHead>
                                    <TableHead>서비스명</TableHead>
                                    <TableHead className="w-[100px]">출처</TableHead>
                                    <TableHead>문의처</TableHead>
                                    <TableHead className="w-[150px]">카테고리</TableHead>
                                </TableRow>
                            </TableHeader>
                            <TableBody>
                                {loading ? (
                                    <TableRow>
                                        <TableCell colSpan={5} className="text-center py-8">
                                            로딩 중...
                                        </TableCell>
                                    </TableRow>
                                ) : welfareList.length === 0 ? (
                                    <TableRow>
                                        <TableCell colSpan={5} className="text-center py-8 text-muted-foreground">
                                            데이터가 없습니다. 동기화를 진행해주세요.
                                        </TableCell>
                                    </TableRow>
                                ) : (
                                    welfareList.map((welfare) => (
                                        <TableRow key={welfare.id}>
                                            <TableCell>{welfare.id}</TableCell>
                                            <TableCell className="font-medium">
                                                {welfare.servNm}
                                                <div className="text-xs text-muted-foreground truncate max-w-[300px]">
                                                    {welfare.servDgst}
                                                </div>
                                            </TableCell>
                                            <TableCell>{getSourceBadge(welfare.source)}</TableCell>
                                            <TableCell>{welfare.rprsCtadr || "-"}</TableCell>
                                            <TableCell>{welfare.category || "-"}</TableCell>
                                        </TableRow>
                                    ))
                                )}
                            </TableBody>
                        </Table>

                        {/* Pagination */}
                        <div className="flex justify-center gap-2 mt-4">
                            <Button
                                variant="outline"
                                size="sm"
                                onClick={() => setPage((p) => Math.max(0, p - 1))}
                                disabled={page === 0}
                            >
                                이전
                            </Button>
                            <span className="flex items-center text-sm">
                                {page + 1} / {totalPages || 1}
                            </span>
                            <Button
                                variant="outline"
                                size="sm"
                                onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                                disabled={page >= totalPages - 1}
                            >
                                다음
                            </Button>
                        </div>
                    </CardContent>
                </Card>
            </div>
        </DashboardLayout>
    );
}
