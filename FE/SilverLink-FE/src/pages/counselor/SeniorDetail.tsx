import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import DashboardLayout from "@/components/layout/DashboardLayout";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Progress } from "@/components/ui/progress";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Label } from "@/components/ui/label";
import {
  Phone,
  Video,
  MapPin,
  Calendar,
  Clock,
  Heart,
  Activity,
  AlertTriangle,
  FileText,
  ChevronLeft,
  Mic,
  MessageCircle,
  TrendingUp,
  User,
  Plus,
  CheckCircle2,
  Pill,
  Sunrise,
  Sun,
  Sunset,
  Moon,
  Loader2
} from "lucide-react";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  RadarChart,
  PolarGrid,
  PolarAngleAxis,
  PolarRadiusAxis,
  Radar,
} from "recharts";
import { counselorNavItems } from "@/config/counselorNavItems";
import { useAuth } from "@/contexts/AuthContext";
import elderlyApi from "@/api/elderly";
import medicationsApi, { MedicationResponse } from "@/api/medications";
import counselingApi from "@/api/counseling";
import callReviewsApi from "@/api/callReviews";
import { ElderlySummaryResponse, HealthInfoResponse, CounselingRecordResponse, CallRecordSummaryResponse } from "@/types/api";

// Mock data
const seniorData = {
  id: "1",
  name: "김영숙",
  age: 78,
  gender: "여성",
  phone: "010-1234-5678",
  address: "서울시 강남구 역삼동 123-45",
  registeredDate: "2024-01-15",
  guardian: {
    name: "김민수",
    relation: "아들",
    phone: "010-9876-5432",
  },
  healthInfo: {
    bloodPressure: "130/85",
    bloodSugar: "110",
    weight: "58kg",
    height: "155cm",
    diseases: ["고혈압", "당뇨(경증)"],
    medications: [
      { name: "아모디핀", dosage: "5mg", frequency: "1일 1회", time: "아침" },
      { name: "메트포르민", dosage: "500mg", frequency: "1일 2회", time: "아침/저녁" },
    ],
    allergies: ["페니실린"],
    lastCheckup: "2024-12-15",
  },
  emotionTrend: [
    { date: "12/20", score: 72, anxiety: 25, depression: 18 },
    { date: "12/21", score: 68, anxiety: 30, depression: 22 },
    { date: "12/22", score: 75, anxiety: 20, depression: 15 },
    { date: "12/23", score: 70, anxiety: 28, depression: 20 },
    { date: "12/24", score: 82, anxiety: 15, depression: 12 },
    { date: "12/25", score: 85, anxiety: 12, depression: 10 },
    { date: "12/26", score: 78, anxiety: 18, depression: 14 },
  ],
  aiAnalysis: {
    overallScore: 78,
    emotionState: "양호",
    riskLevel: "낮음",
    keywords: ["가족", "건강", "외로움", "식사"],
    concerns: [
      { type: "경미", content: "최근 식사량 감소 언급" },
      { type: "관찰", content: "손자 방문 후 기분 호전" },
    ],
    recommendations: [
      "정기적인 가족 연락 권장",
      "식사 패턴 모니터링 필요",
      "외출 활동 격려",
    ],
    voiceAnalysis: {
      clarity: 85,
      energy: 72,
      stability: 88,
      responseTime: 1.2,
    },
  },
  recentCalls: [
    { date: "2024-12-26", duration: "8분 32초", emotion: "좋음", summary: "손자 방문 이야기, 식사 잘 함" },
    { date: "2024-12-25", duration: "12분 15초", emotion: "매우 좋음", summary: "크리스마스 가족 모임, 기분 좋음" },
    { date: "2024-12-24", duration: "6분 48초", emotion: "보통", summary: "날씨 이야기, 약간 외로움 표현" },
  ],
  counselingRecords: [
    {
      id: "1",
      date: "2024-12-26",
      counselor: "이상담",
      type: "정기 상담",
      content: "어르신 상태 양호. 손자 방문 후 기분이 많이 좋아지셨다고 함. 식사량도 증가.",
      followUp: "다음 주 복지관 프로그램 참여 권유",
    },
    {
      id: "2",
      date: "2024-12-20",
      counselor: "이상담",
      type: "건강 체크",
      content: "혈압 측정 결과 정상 범위. 복약 잘 하고 계심. 약간의 수면 장애 호소.",
      followUp: "수면 패턴 모니터링, 필요시 의료진 연계",
    },
  ],
};

const radarData = [
  { subject: "음성 명료도", A: 85, fullMark: 100 },
  { subject: "대화 에너지", A: 72, fullMark: 100 },
  { subject: "정서 안정성", A: 88, fullMark: 100 },
  { subject: "반응 속도", A: 80, fullMark: 100 },
  { subject: "대화 참여도", A: 75, fullMark: 100 },
];

export default function SeniorDetail() {
  const { user } = useAuth();
  const { id } = useParams();
  const navigate = useNavigate();
  const numericId = Number(id);

  const [isLoading, setIsLoading] = useState(true);
  const [profile, setProfile] = useState<ElderlySummaryResponse | null>(null);
  const [healthInfo, setHealthInfo] = useState<HealthInfoResponse | null>(null);
  const [medications, setMedications] = useState<MedicationResponse[]>([]);
  const [counselingRecords, setCounselingRecords] = useState<CounselingRecordResponse[]>([]);
  const [callRecords, setCallRecords] = useState<CallRecordSummaryResponse[]>([]);

  const [activeTab, setActiveTab] = useState("overview");
  const [newRecord, setNewRecord] = useState({
    type: "정기 상담",
    content: "",
    followUp: "",
  });
  const [isEditing, setIsEditing] = useState(false);

  useEffect(() => {
    if (!numericId) return;

    const fetchData = async () => {
      try {
        setIsLoading(true);

        // 프로필 정보는 필수
        const profileData = await elderlyApi.getSummary(numericId);
        setProfile(profileData);

        // 건강 정보와 복약 정보는 선택적 (에러 발생해도 계속 진행)
        try {
          const healthData = await elderlyApi.getHealthInfo(numericId);
          setHealthInfo(healthData);
        } catch (error) {
          console.warn("건강 정보 조회 실패 (권한 없음 또는 데이터 없음):", error);
          setHealthInfo(null);
        }

        try {
          const medicationData = await medicationsApi.getMedicationsByElderly(numericId);
          setMedications(medicationData);
        } catch (error) {
          console.warn("복약 정보 조회 실패:", error);
          setMedications([]);
        }

        try {
          const counselingData = await counselingApi.getRecordsByElderly(numericId);
          setCounselingRecords(counselingData);
        } catch (error) {
          console.warn("상담 기록 조회 실패:", error);
          setCounselingRecords([]);
        }

        try {
          const callData = await callReviewsApi.getCallRecordsForCounselor({ page: 0, size: 100 });
          // 해당 어르신의 통화 기록만 필터링
          const filteredCalls = callData.content.filter(call => call.elderlyId === numericId);
          setCallRecords(filteredCalls);
        } catch (error) {
          console.warn("통화 기록 조회 실패:", error);
          setCallRecords([]);
        }
      } catch (error) {
        console.error("Failed to fetch elderly detail:", error);
      } finally {
        setIsLoading(false);
      }
    };

    fetchData();
  }, [numericId]);

  // Mock data for charts (merged with real name)
  const chartData = {
    emotionTrend: seniorData.emotionTrend,
    aiAnalysis: seniorData.aiAnalysis,
    recentCalls: seniorData.recentCalls,
    counselingRecords: seniorData.counselingRecords,
  };

  const handleSaveRecord = () => {
    // Saving record...
    setNewRecord({ type: "정기 상담", content: "", followUp: "" });
    setIsEditing(false);
  };

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

  if (isLoading) {
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

  if (!profile) {
    return (
      <DashboardLayout
        role="counselor"
        userName={user?.name || "상담사"}
        userAvatar=""
        navItems={counselorNavItems}
      >
        <div className="flex items-center justify-center h-64">
          <p className="text-muted-foreground">어르신 정보를 찾을 수 없습니다.</p>
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
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <Button variant="ghost" onClick={() => navigate("/counselor/seniors")} className="w-fit p-0 hover:bg-transparent text-muted-foreground hover:text-foreground">
            <ChevronLeft className="w-4 h-4 mr-2" />
            목록으로 돌아가기
          </Button>
          <div className="flex gap-2">
            <Button variant="outline" className="gap-2">
              <Phone className="w-4 h-4" />
              전화 걸기
            </Button>
            <Button variant="outline" className="gap-2">
              <Video className="w-4 h-4" />
              영상 통화
            </Button>
            <Button className="gap-2">
              <FileText className="w-4 h-4" />
              상담 기록 작성
            </Button>
          </div>
        </div>

        {/* Profile Card */}
        <Card className="border-0 shadow-lg bg-gradient-to-r from-primary/5 via-background to-background">
          <CardContent className="p-6">
            <div className="flex flex-col md:flex-row gap-6 items-start">
              <div className="relative">
                <Avatar className="w-24 h-24 border-4 border-background shadow-xl">
                  <AvatarImage src={`https://api.dicebear.com/7.x/avataaars/svg?seed=${profile.name}`} />
                  <AvatarFallback>{profile.name.charAt(0)}</AvatarFallback>
                </Avatar>
                <div className="absolute -bottom-2 right-2 bg-green-500 w-5 h-5 rounded-full border-2 border-background" />
              </div>

              <div className="flex-1 space-y-4">
                <div>
                  <div className="flex items-center gap-3">
                    <h1 className="text-2xl font-bold">{profile.name}</h1>
                    <Badge variant="secondary" className="text-base font-normal">
                      {profile.age}세 / {profile.gender === "M" ? "남성" : "여성"}
                    </Badge>
                    <Badge className={getRiskBadge(chartData.aiAnalysis.riskLevel)}>
                      위험도 {chartData.aiAnalysis.riskLevel}
                    </Badge>
                  </div>
                  <div className="flex items-center gap-4 mt-2 text-muted-foreground">
                    <div className="flex items-center gap-1">
                      <MapPin className="w-4 h-4" />
                      {profile.fullAddress || "-"}
                    </div>
                    <div className="flex items-center gap-1">
                      <Calendar className="w-4 h-4" />
                      생년월일: {profile.birthDate || "-"}
                    </div>
                  </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-4 p-4 bg-background/50 rounded-xl border">
                  <div>
                    <p className="text-sm text-muted-foreground mb-1">보호자</p>
                    <div className="font-medium flex items-center gap-2">
                      <User className="w-4 h-4" />
                      {profile.guardianName || "미지정"}
                    </div>
                    {/* <p className="text-sm text-muted-foreground mt-1">{seniorData.guardian.phone}</p> */}
                  </div>
                  <div>
                    <p className="text-sm text-muted-foreground mb-1">최근 건강검진</p>
                    <div className="font-medium flex items-center gap-2">
                      <Activity className="w-4 h-4" />
                      -
                    </div>
                    <p className="text-sm text-muted-foreground mt-1">
                      혈압 {healthInfo?.bloodPressure || "-"}
                    </p>
                  </div>
                  <div>
                    <p className="text-sm text-muted-foreground mb-1">최근 감정상태</p>
                    <div className="font-medium flex items-center gap-2">
                      <Heart className="w-4 h-4" />
                      {chartData.aiAnalysis.emotionState} ({chartData.aiAnalysis.overallScore}점)
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </CardContent>
        </Card>

        <Tabs value={activeTab} onValueChange={setActiveTab} className="space-y-6">
          <TabsList className="grid w-full grid-cols-3 lg:w-[300px]">
            <TabsTrigger value="overview">종합 분석</TabsTrigger>
            <TabsTrigger value="health">건강 정보</TabsTrigger>
            <TabsTrigger value="records">상담 기록</TabsTrigger>
          </TabsList>

          <TabsContent value="overview" className="space-y-6">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {/* Emotion Trend Chart */}
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2">
                    <TrendingUp className="w-5 h-5 text-primary" />
                    감정 변화 추이
                  </CardTitle>
                  <CardDescription>최근 7일간의 감정 점수 변화입니다</CardDescription>
                </CardHeader>
                <CardContent>
                  <div className="h-[300px]">
                    <ResponsiveContainer width="100%" height="100%">
                      <LineChart data={chartData.emotionTrend}>
                        <CartesianGrid strokeDasharray="3 3" vertical={false} />
                        <XAxis dataKey="date" />
                        <YAxis domain={[0, 100]} />
                        <Tooltip />
                        <Line
                          type="monotone"
                          dataKey="score"
                          stroke="#2563eb"
                          strokeWidth={2}
                          name="종합 점수"
                        />
                        <Line
                          type="monotone"
                          dataKey="anxiety"
                          stroke="#e11d48"
                          strokeWidth={2}
                          name="불안도"
                        />
                      </LineChart>
                    </ResponsiveContainer>
                  </div>
                </CardContent>
              </Card>

              {/* Voice Analysis Radar */}
              <Card>
                <CardHeader>
                  <CardTitle className="flex items-center gap-2">
                    <Mic className="w-5 h-5 text-primary" />
                    음성 분석 리포트
                  </CardTitle>
                  <CardDescription>최근 통화 음성 분석 결과입니다</CardDescription>
                </CardHeader>
                <CardContent>
                  <div className="h-[300px]">
                    <ResponsiveContainer width="100%" height="100%">
                      <RadarChart cx="50%" cy="50%" outerRadius="80%" data={radarData}>
                        <PolarGrid />
                        <PolarAngleAxis dataKey="subject" />
                        <PolarRadiusAxis domain={[0, 100]} />
                        <Radar
                          name="현재 상태"
                          dataKey="A"
                          stroke="#2563eb"
                          fill="#2563eb"
                          fillOpacity={0.6}
                        />
                        <Tooltip />
                      </RadarChart>
                    </ResponsiveContainer>
                  </div>
                </CardContent>
              </Card>

              {/* AI Key Insights */}
              <Card className="md:col-span-2">
                <CardHeader>
                  <CardTitle className="flex items-center gap-2">
                    <Activity className="w-5 h-5 text-primary" />
                    AI 주요 분석
                  </CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="grid md:grid-cols-3 gap-6">
                    <div className="space-y-4">
                      <h4 className="font-semibold flex items-center gap-2">
                        <AlertTriangle className="w-4 h-4 text-warning" />
                        주요 우려사항
                      </h4>
                      <div className="space-y-2">
                        {chartData.aiAnalysis.concerns.map((item, idx) => (
                          <div key={idx} className="flex gap-2 p-3 bg-muted/50 rounded-lg text-sm">
                            <Badge variant="outline">{item.type}</Badge>
                            <span>{item.content}</span>
                          </div>
                        ))}
                      </div>
                    </div>
                    <div className="space-y-4">
                      <h4 className="font-semibold flex items-center gap-2">
                        <CheckCircle2 className="w-4 h-4 text-success" />
                        추천 조치사항
                      </h4>
                      <ul className="space-y-2">
                        {chartData.aiAnalysis.recommendations.map((item, idx) => (
                          <li key={idx} className="flex items-center gap-2 text-sm p-3 bg-success/5 rounded-lg border border-success/10">
                            <div className="w-1.5 h-1.5 rounded-full bg-success" />
                            {item}
                          </li>
                        ))}
                      </ul>
                    </div>
                    <div className="space-y-4">
                      <h4 className="font-semibold flex items-center gap-2">
                        <MessageCircle className="w-4 h-4 text-blue-500" />
                        주요 키워드
                      </h4>
                      <div className="flex flex-wrap gap-2">
                        {chartData.aiAnalysis.keywords.map((keyword, idx) => (
                          <Badge key={idx} variant="secondary" className="px-3 py-1">
                            #{keyword}
                          </Badge>
                        ))}
                      </div>
                    </div>
                  </div>
                </CardContent>
              </Card>

              {/* Recent Calls List */}
              <Card className="md:col-span-2">
                <CardHeader>
                  <CardTitle>최근 통화 기록</CardTitle>
                </CardHeader>
                <CardContent>
                  <div className="space-y-4">
                    {callRecords.length > 0 ? callRecords.slice(0, 10).map((call) => {
                      // 통화 시간 포맷팅
                      const formatDuration = (seconds: number) => {
                        const mins = Math.floor(seconds / 60);
                        const secs = seconds % 60;
                        if (mins > 0) {
                          return `${mins}분 ${secs}초`;
                        }
                        return `${secs}초`;
                      };

                      // 감정 한글 변환
                      const emotionMap: Record<string, string> = {
                        'GOOD': '좋음',
                        'NORMAL': '보통',
                        'BAD': '나쁨',
                        'VERY_GOOD': '매우 좋음',
                        'VERY_BAD': '매우 나쁨'
                      };
                      const emotionLabel = emotionMap[call.emotionSummary] || call.emotionSummary || '보통';

                      return (
                        <div key={call.callId} className="flex items-center justify-between p-4 border rounded-xl hover:bg-muted/50 transition-colors cursor-pointer" onClick={() => navigate(`/counselor/calls/${call.callId}`)}>
                          <div className="flex items-center gap-4">
                            <div className="w-10 h-10 rounded-full bg-primary/10 flex items-center justify-center">
                              <Phone className="w-5 h-5 text-primary" />
                            </div>
                            <div>
                              <div className="flex items-center gap-2">
                                <span className="font-semibold">{call.callAt?.split('T')[0] || '-'}</span>
                                <span className="text-sm text-muted-foreground">({formatDuration(call.durationSeconds || 0)})</span>
                              </div>
                              <p className="text-sm text-muted-foreground mt-1 line-clamp-1">{call.summary || '통화 내용 없음'}</p>
                            </div>
                          </div>
                          <Badge className={`${getEmotionBadge(emotionLabel)} border-0`}>
                            감정: {emotionLabel}
                          </Badge>
                        </div>
                      );
                    }) : (
                      <div className="text-center py-8 text-muted-foreground">
                        <Phone className="w-8 h-8 mx-auto mb-2 opacity-50" />
                        <p>통화 기록이 없습니다.</p>
                      </div>
                    )}
                  </div>
                </CardContent>
              </Card>
            </div>
          </TabsContent>

          <TabsContent value="health">
            <Card>
              <CardHeader>
                <CardTitle>건강 상세 정보</CardTitle>
                <CardDescription>보유 질환 및 복약 정보입니다</CardDescription>
              </CardHeader>
              <CardContent className="space-y-6">
                <div className="grid md:grid-cols-2 gap-6">
                  <div className="space-y-4">
                    <h3 className="font-semibold text-lg flex items-center gap-2">
                      <Activity className="w-5 h-5" />
                      신체 계측 및 질환
                    </h3>
                    <div className="grid grid-cols-2 gap-4">
                      <div className="p-4 bg-muted/30 rounded-lg">
                        <p className="text-sm text-muted-foreground">보유 질환</p>
                        <div className="flex flex-wrap gap-2 mt-2">
                          {healthInfo?.diseases?.length ? healthInfo.diseases.map((d, i) => (
                            <Badge key={i} variant="outline">{d}</Badge>
                          )) : <span className="text-sm text-muted-foreground">-</span>}
                        </div>
                      </div>
                      <div className="p-4 bg-muted/30 rounded-lg">
                        <p className="text-sm text-muted-foreground">알레르기 (정보 없음)</p>
                        <div className="flex flex-wrap gap-2 mt-2">
                          {/* {seniorData.healthInfo.allergies.map((a, i) => (
                            <Badge key={i} variant="destructive" className="bg-destructive/10 text-destructive border-0">{a}</Badge>
                          ))} */}
                          <span className="text-sm text-muted-foreground">-</span>
                        </div>
                      </div>
                    </div>
                  </div>

                  <div className="space-y-4">
                    <h3 className="font-semibold text-lg flex items-center gap-2">
                      <Pill className="w-5 h-5" />
                      복약 정보
                    </h3>
                    <div className="space-y-3">
                      {medications.length > 0 ? medications.map((med) => (
                        <div key={med.id} className="p-4 bg-muted/30 rounded-lg space-y-3 border border-muted">
                          <div className="flex items-start justify-between">
                            <div className="flex-1">
                              <div className="flex items-center gap-2">
                                <Pill className="w-4 h-4 text-primary" />
                                <p className="font-semibold text-base">{med.name}</p>
                              </div>
                              {med.dosage && (
                                <p className="text-sm text-muted-foreground mt-1 ml-6">{med.dosage}</p>
                              )}
                            </div>
                            <Badge variant={med.reminder ? "default" : "secondary"} className="gap-1">
                              <Clock className="w-3 h-3" />
                              {med.reminder ? "알림 ON" : "알림 OFF"}
                            </Badge>
                          </div>

                          {/* 복용 시간 */}
                          <div className="flex flex-wrap gap-2 ml-6">
                            {med.times.map((time) => {
                              const timeConfig = {
                                morning: { icon: Sunrise, label: "아침", time: "08:00" },
                                noon: { icon: Sun, label: "점심", time: "12:00" },
                                evening: { icon: Sunset, label: "저녁", time: "18:00" },
                                night: { icon: Moon, label: "취침전", time: "22:00" },
                              }[time] || { icon: Clock, label: time, time: "-" };

                              const Icon = timeConfig.icon;

                              return (
                                <Badge key={time} variant="outline" className="gap-1.5 py-1 px-3">
                                  <Icon className="w-3.5 h-3.5" />
                                  {timeConfig.label} ({timeConfig.time})
                                </Badge>
                              );
                            })}
                          </div>

                          {/* 복용 방법 */}
                          {med.instructions && (
                            <div className="ml-6 text-sm text-muted-foreground bg-blue-50 dark:bg-blue-900/20 p-2 rounded">
                              💊 {med.instructions}
                            </div>
                          )}

                          {/* 복용 기간 */}
                          {(med.startDate || med.endDate) && (
                            <div className="ml-6 text-xs text-muted-foreground flex items-center gap-2">
                              <Calendar className="w-3 h-3" />
                              {med.startDate && <span>시작: {med.startDate}</span>}
                              {med.endDate && <span>~ 종료: {med.endDate}</span>}
                            </div>
                          )}
                        </div>
                      )) : (
                        <div className="p-6 bg-muted/30 rounded-lg text-center">
                          <Pill className="w-8 h-8 mx-auto text-muted-foreground mb-2" />
                          <p className="text-sm text-muted-foreground">등록된 복약 정보가 없습니다.</p>
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>
          </TabsContent>

          <TabsContent value="records">
            <Card>
              <CardHeader className="flex flex-row items-center justify-between">
                <div>
                  <CardTitle>상담 기록</CardTitle>
                  <CardDescription>과거 상담 내역 및 조치사항</CardDescription>
                </div>
                <Button onClick={() => setIsEditing(true)}>
                  <Plus className="w-4 h-4 mr-2" />
                  새 상담 기록
                </Button>
              </CardHeader>
              <CardContent>
                {isEditing && (
                  <Card className="mb-6 border-2 border-primary/20">
                    <CardHeader>
                      <CardTitle className="text-lg">새 상담 기록 작성</CardTitle>
                    </CardHeader>
                    <CardContent className="space-y-4">
                      <div className="space-y-2">
                        <Label>상담 유형</Label>
                        <Select
                          value={newRecord.type}
                          onValueChange={(val) => setNewRecord({ ...newRecord, type: val })}
                        >
                          <SelectTrigger>
                            <SelectValue />
                          </SelectTrigger>
                          <SelectContent>
                            <SelectItem value="정기 상담">정기 상담</SelectItem>
                            <SelectItem value="건강 체크">건강 체크</SelectItem>
                            <SelectItem value="긴급 대응">긴급 대응</SelectItem>
                          </SelectContent>
                        </Select>
                      </div>
                      <div className="space-y-2">
                        <Label>상담 내용</Label>
                        <Textarea
                          value={newRecord.content}
                          onChange={(e) => setNewRecord({ ...newRecord, content: e.target.value })}
                          placeholder="상담 내용을 입력하세요..."
                          rows={4}
                        />
                      </div>
                      <div className="space-y-2">
                        <Label>후속 조치</Label>
                        <Input
                          value={newRecord.followUp}
                          onChange={(e) => setNewRecord({ ...newRecord, followUp: e.target.value })}
                          placeholder="필요한 후속 조치를 입력하세요"
                        />
                      </div>
                      <div className="flex justify-end gap-2">
                        <Button variant="ghost" onClick={() => setIsEditing(false)}>취소</Button>
                        <Button onClick={handleSaveRecord}>저장</Button>
                      </div>
                    </CardContent>
                  </Card>
                )}

                <div className="space-y-4">
                  {counselingRecords.length > 0 ? counselingRecords.map((record) => {
                    // 타입 한글 변환
                    const typeMap: Record<string, string> = {
                      'PHONE': '전화 상담',
                      'VISIT': '방문 상담',
                      'VIDEO': '영상 상담'
                    };
                    const typeLabel = typeMap[record.type] || record.type;

                    return (
                      <div key={record.id} className="relative pl-8 pb-8 border-l last:pb-0">
                        <div className="absolute left-[-5px] top-0 w-2.5 h-2.5 rounded-full bg-primary" />
                        <div className="flex flex-col sm:flex-row sm:items-center justify-between mb-2">
                          <div className="flex items-center gap-2">
                            <span className="font-semibold text-lg">{record.date}</span>
                            <Badge variant="outline">{typeLabel}</Badge>
                          </div>
                          <span className="text-sm text-muted-foreground">카테고리: {record.category || '-'}</span>
                        </div>
                        <div className="p-4 bg-muted/30 rounded-lg space-y-3">
                          <p className="text-foreground">{record.content || record.summary || '내용 없음'}</p>
                          {record.followUp && (
                            <div className="flex items-start gap-2 text-sm text-blue-600 bg-blue-50 dark:bg-blue-900/20 p-2 rounded">
                              <CheckCircle2 className="w-4 h-4 mt-0.5" />
                              <span>후속 조치: {record.followUp}</span>
                            </div>
                          )}
                        </div>
                      </div>
                    );
                  }) : (
                    <div className="text-center py-8 text-muted-foreground">
                      <FileText className="w-8 h-8 mx-auto mb-2 opacity-50" />
                      <p>상담 기록이 없습니다.</p>
                    </div>
                  )}
                </div>
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>
      </div>
    </DashboardLayout>
  );
}

// Missing imports removed
