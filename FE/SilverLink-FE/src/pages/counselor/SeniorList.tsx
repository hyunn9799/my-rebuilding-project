import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import DashboardLayout from "@/components/layout/DashboardLayout";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  User,
  Search,
  Filter,
  Phone,
  Activity,
  AlertTriangle,
  ChevronRight,
  Loader2,
} from "lucide-react";
import { counselorNavItems } from "@/config/counselorNavItems";
import { toast } from "sonner";
import { useAuth } from "@/contexts/AuthContext";
import assignmentsApi, { AssignmentResponse } from "@/api/assignments";
import counselorsApi from "@/api/counselors";
import elderlyApi from "@/api/elderly";

interface Senior {
  id: string;
  name: string;
  age?: number;
  gender?: string;
  phone?: string;
  address?: string;
  lastCall?: string;
  emotionState: string;
  riskLevel: string;
  guardian?: string;
}

export default function SeniorList() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [searchTerm, setSearchTerm] = useState("");
  const [riskFilter, setRiskFilter] = useState("all");
  const [emotionFilter, setEmotionFilter] = useState("all");
  const [seniors, setSeniors] = useState<Senior[]>([]);
  const [loading, setLoading] = useState(true);
  const [counselorName, setCounselorName] = useState(user?.name || "상담사");

  // 데이터 로드
  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);

        // 상담사 정보 조회 (Deleted)


        // 배정된 어르신 목록 조회
        const assignments = await assignmentsApi.getMyAssignments();

        // 배정된 어르신 상세 정보 조회
        const activeAssignments = assignments.filter(a => a.status === 'ACTIVE');
        const seniorPromises = activeAssignments.map(async (assignment) => {
          try {
            const summary = await elderlyApi.getSummary(assignment.elderlyId);
            return {
              id: String(assignment.elderlyId),
              name: summary.name,
              age: summary.age,
              gender: summary.gender === 'M' ? '남성' : '여성',
              phone: summary.phone,
              address: summary.fullAddress || (summary.sidoName ? `${summary.sidoName} ${summary.sigunguName}` : '-'),
              guardian: summary.guardianName || '-',
              lastCall: '-',
              emotionState: "보통",
              riskLevel: "낮음",
            };
          } catch (e) {
            console.error(`Failed to fetch summary for elderly ${assignment.elderlyId}`, e);
            return {
              id: String(assignment.elderlyId),
              name: assignment.elderlyName,
              emotionState: "보통",
              riskLevel: "낮음",
              age: undefined,
              gender: undefined,
              phone: undefined,
              address: undefined,
              guardian: undefined,
              lastCall: undefined
            };
          }
        });

        const seniorList = await Promise.all(seniorPromises);
        setSeniors(seniorList);
      } catch (error) {
        console.error("데이터 로드 실패:", error);
        toast.error("데이터를 불러오지 못했습니다.", {
          description: "잠시 후 다시 시도해주세요."
        });
        // 로그인되지 않은 경우 등 오류 시 빈 목록 표시
        setSeniors([]);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  const filteredSeniors = seniors.filter((senior) => {
    const matchesSearch =
      senior.name.includes(searchTerm) ||
      (senior.phone?.includes(searchTerm) ?? false) ||
      (senior.address?.includes(searchTerm) ?? false);
    const matchesRisk = riskFilter === "all" || senior.riskLevel === riskFilter;
    const matchesEmotion = emotionFilter === "all" || senior.emotionState === emotionFilter;
    return matchesSearch && matchesRisk && matchesEmotion;
  });

  const getEmotionBadge = (emotion: string) => {
    const variants: Record<string, string> = {
      "매우 좋음": "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/30 dark:text-emerald-400",
      "좋음": "bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400",
      "보통": "bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400",
      "나쁨": "bg-orange-100 text-orange-700 dark:bg-orange-900/30 dark:text-orange-400",
      "매우 나쁨": "bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400",
    };
    return variants[emotion] || variants["보통"];
  };

  const getRiskBadge = (level: string) => {
    const variants: Record<string, string> = {
      "낮음": "bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400",
      "보통": "bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400",
      "높음": "bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400",
    };
    return variants[level] || variants["보통"];
  };

  if (loading) {
    return (
      <DashboardLayout
        role="counselor"
        userName={user?.name || "상담사"}
        userAvatar=""
        navItems={counselorNavItems}
      >
        <div className="flex items-center justify-center h-64">
          <Loader2 className="w-8 h-8 animate-spin text-primary" />
        </div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout
      role="counselor"
      userName={user?.name || "상담사"}
      userAvatar=""
      navItems={counselorNavItems}
    >
      <div className="space-y-6">
        {/* Header */}
        <div>
          <h1 className="text-2xl font-bold text-foreground">담당 어르신 목록</h1>
          <p className="text-muted-foreground">담당하고 계신 어르신들의 정보를 확인하세요</p>
        </div>

        {/* Stats */}
        <div className="grid gap-4 md:grid-cols-4">
          <Card>
            <CardContent className="pt-6">
              <div className="flex items-center gap-3">
                <div className="p-2 rounded-full bg-primary/10">
                  <User className="h-5 w-5 text-primary" />
                </div>
                <div>
                  <p className="text-2xl font-bold">{seniors.length}</p>
                  <p className="text-sm text-muted-foreground">전체 어르신</p>
                </div>
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="pt-6">
              <div className="flex items-center gap-3">
                <div className="p-2 rounded-full bg-red-100 dark:bg-red-900/30">
                  <AlertTriangle className="h-5 w-5 text-red-600 dark:text-red-400" />
                </div>
                <div>
                  <p className="text-2xl font-bold">
                    {seniors.filter((s) => s.riskLevel === "높음").length}
                  </p>
                  <p className="text-sm text-muted-foreground">고위험</p>
                </div>
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="pt-6">
              <div className="flex items-center gap-3">
                <div className="p-2 rounded-full bg-yellow-100 dark:bg-yellow-900/30">
                  <Activity className="h-5 w-5 text-yellow-600 dark:text-yellow-400" />
                </div>
                <div>
                  <p className="text-2xl font-bold">
                    {seniors.filter((s) => s.riskLevel === "보통").length}
                  </p>
                  <p className="text-sm text-muted-foreground">주의 필요</p>
                </div>
              </div>
            </CardContent>
          </Card>
          <Card>
            <CardContent className="pt-6">
              <div className="flex items-center gap-3">
                <div className="p-2 rounded-full bg-green-100 dark:bg-green-900/30">
                  <Phone className="h-5 w-5 text-green-600 dark:text-green-400" />
                </div>
                <div>
                  <p className="text-2xl font-bold">
                    {seniors.filter((s) => s.lastCall === new Date().toISOString().split('T')[0]).length}
                  </p>
                  <p className="text-sm text-muted-foreground">오늘 통화 완료</p>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Filters */}
        <Card>
          <CardContent className="pt-6">
            <div className="flex flex-col gap-4 md:flex-row md:items-center">
              <div className="relative flex-1">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                <Input
                  placeholder="이름, 연락처, 주소로 검색..."
                  className="pl-9"
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                />
              </div>
              <div className="flex gap-2">
                <Select value={riskFilter} onValueChange={setRiskFilter}>
                  <SelectTrigger className="w-[140px]">
                    <Filter className="h-4 w-4 mr-2" />
                    <SelectValue placeholder="위험도" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="all">전체 위험도</SelectItem>
                    <SelectItem value="높음">높음</SelectItem>
                    <SelectItem value="보통">보통</SelectItem>
                    <SelectItem value="낮음">낮음</SelectItem>
                  </SelectContent>
                </Select>
                <Select value={emotionFilter} onValueChange={setEmotionFilter}>
                  <SelectTrigger className="w-[140px]">
                    <SelectValue placeholder="감정 상태" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="all">전체 감정</SelectItem>
                    <SelectItem value="매우 좋음">매우 좋음</SelectItem>
                    <SelectItem value="좋음">좋음</SelectItem>
                    <SelectItem value="보통">보통</SelectItem>
                    <SelectItem value="나쁨">나쁨</SelectItem>
                    <SelectItem value="매우 나쁨">매우 나쁨</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* Table */}
        <Card>
          <CardHeader>
            <CardTitle>어르신 목록 ({filteredSeniors.length}명)</CardTitle>
          </CardHeader>
          <CardContent>
            {seniors.length === 0 ? (
              <div className="text-center py-8 text-muted-foreground">
                배정된 어르신이 없습니다.
              </div>
            ) : (
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>이름</TableHead>
                    <TableHead>나이/성별</TableHead>
                    <TableHead>연락처</TableHead>
                    <TableHead>지역</TableHead>
                    <TableHead>보호자</TableHead>
                    <TableHead>최근 통화</TableHead>
                    <TableHead>감정 상태</TableHead>
                    <TableHead>위험도</TableHead>
                    <TableHead></TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {filteredSeniors.map((senior) => (
                    <TableRow
                      key={senior.id}
                      className="cursor-pointer hover:bg-muted/50"
                      onClick={() => navigate(`/counselor/seniors/${senior.id}`)}
                    >
                      <TableCell className="font-medium">{senior.name}</TableCell>
                      <TableCell>
                        {senior.age ? `${senior.age}세` : '-'} / {senior.gender || '-'}
                      </TableCell>
                      <TableCell>{senior.phone || '-'}</TableCell>
                      <TableCell>{senior.address || '-'}</TableCell>
                      <TableCell>{senior.guardian || '-'}</TableCell>
                      <TableCell>{senior.lastCall || '-'}</TableCell>
                      <TableCell>
                        <Badge className={getEmotionBadge(senior.emotionState)}>
                          {senior.emotionState}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <Badge className={getRiskBadge(senior.riskLevel)}>
                          {senior.riskLevel}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <ChevronRight className="h-4 w-4 text-muted-foreground" />
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </CardContent>
        </Card>
      </div>
    </DashboardLayout>
  );
}
