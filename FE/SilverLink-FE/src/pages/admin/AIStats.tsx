import { 
  TrendingUp,
  TrendingDown,
  Clock,
  Activity,
  Mic,
  Volume2,
  Brain,
  MessageCircle,
  Zap
} from "lucide-react";
import { adminNavItems } from "@/config/adminNavItems";
import { useAuth } from "@/contexts/AuthContext";
import DashboardLayout from "@/components/layout/DashboardLayout";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Progress } from "@/components/ui/progress";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, BarChart, Bar, RadarChart, PolarGrid, PolarAngleAxis, PolarRadiusAxis, Radar } from "recharts";

const performanceData = [
  { date: "01/01", stt: 95.2, tts: 93.8, qa: 91.5, response: 97.2 },
  { date: "01/02", stt: 95.8, tts: 94.1, qa: 92.0, response: 97.5 },
  { date: "01/03", stt: 96.1, tts: 94.5, qa: 91.8, response: 97.8 },
  { date: "01/04", stt: 95.5, tts: 94.2, qa: 92.3, response: 98.0 },
  { date: "01/05", stt: 96.3, tts: 94.8, qa: 92.5, response: 98.2 },
  { date: "01/06", stt: 96.0, tts: 95.0, qa: 93.0, response: 98.1 },
  { date: "01/07", stt: 96.2, tts: 94.8, qa: 92.1, response: 98.5 },
];

const dailyCallData = [
  { hour: "06시", calls: 12 },
  { hour: "08시", calls: 45 },
  { hour: "10시", calls: 128 },
  { hour: "12시", calls: 85 },
  { hour: "14시", calls: 142 },
  { hour: "16시", calls: 156 },
  { hour: "18시", calls: 98 },
  { hour: "20시", calls: 42 },
];

const radarData = [
  { metric: "STT 정확도", value: 96.2, fullMark: 100 },
  { metric: "TTS 품질", value: 94.8, fullMark: 100 },
  { metric: "Q&A 정확도", value: 92.1, fullMark: 100 },
  { metric: "응답 시간", value: 98.5, fullMark: 100 },
  { metric: "감정 분석", value: 91.5, fullMark: 100 },
  { metric: "키워드 추출", value: 93.8, fullMark: 100 },
];

const metrics = [
  { 
    name: "STT (음성→텍스트)", 
    value: 96.2, 
    change: 1.2, 
    icon: <Mic className="w-5 h-5" />,
    color: "text-primary",
    bgColor: "bg-primary/10",
    description: "음성 인식 정확도"
  },
  { 
    name: "TTS (텍스트→음성)", 
    value: 94.8, 
    change: 0.5, 
    icon: <Volume2 className="w-5 h-5" />,
    color: "text-accent",
    bgColor: "bg-accent/10",
    description: "음성 합성 품질"
  },
  { 
    name: "Q&A 정확도", 
    value: 92.1, 
    change: -0.4, 
    icon: <MessageCircle className="w-5 h-5" />,
    color: "text-info",
    bgColor: "bg-info/10",
    description: "질문 응답 정확도"
  },
  { 
    name: "응답 시간", 
    value: 98.5, 
    change: 2.1, 
    icon: <Zap className="w-5 h-5" />,
    color: "text-success",
    bgColor: "bg-success/10",
    description: "목표 응답시간 달성률"
  },
];

const recentEvaluations = [
  { id: 1, date: "2024-01-15", totalCalls: 652, avgScore: 94.2, issues: 3 },
  { id: 2, date: "2024-01-14", totalCalls: 648, avgScore: 93.8, issues: 5 },
  { id: 3, date: "2024-01-13", totalCalls: 621, avgScore: 94.5, issues: 2 },
  { id: 4, date: "2024-01-12", totalCalls: 589, avgScore: 93.2, issues: 7 },
  { id: 5, date: "2024-01-11", totalCalls: 634, avgScore: 94.1, issues: 4 },
];

const AIStats = () => {
  const { user } = useAuth();

  return (
    <DashboardLayout
      role="admin"
      userName={user?.name || "관리자"}
      navItems={adminNavItems}
    >
      <div className="space-y-6">
        {/* Page Header */}
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-foreground">AI 성능 통계</h1>
            <p className="text-muted-foreground mt-1">AI 모델의 성능 지표를 모니터링합니다</p>
          </div>
          <Select defaultValue="week">
            <SelectTrigger className="w-[150px]">
              <SelectValue placeholder="기간 선택" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="day">오늘</SelectItem>
              <SelectItem value="week">최근 7일</SelectItem>
              <SelectItem value="month">최근 30일</SelectItem>
            </SelectContent>
          </Select>
        </div>

        {/* Metrics Cards */}
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          {metrics.map((metric) => (
            <Card key={metric.name} className="shadow-card border-0">
              <CardContent className="p-6">
                <div className="flex items-center justify-between mb-4">
                  <div className={`w-10 h-10 rounded-xl ${metric.bgColor} flex items-center justify-center ${metric.color}`}>
                    {metric.icon}
                  </div>
                  <span className={`text-sm flex items-center gap-1 ${metric.change >= 0 ? 'text-success' : 'text-destructive'}`}>
                    {metric.change >= 0 ? <TrendingUp className="w-3 h-3" /> : <TrendingDown className="w-3 h-3" />}
                    {Math.abs(metric.change)}%
                  </span>
                </div>
                <p className="text-3xl font-bold">{metric.value}%</p>
                <p className="text-sm text-muted-foreground mt-1">{metric.name}</p>
              </CardContent>
            </Card>
          ))}
        </div>

        <div className="grid lg:grid-cols-3 gap-6">
          {/* Performance Trend Chart */}
          <div className="lg:col-span-2">
            <Card className="shadow-card border-0">
              <CardHeader>
                <CardTitle className="text-lg">성능 추이</CardTitle>
                <CardDescription>일별 AI 성능 지표 변화</CardDescription>
              </CardHeader>
              <CardContent>
                <div className="h-80">
                  <ResponsiveContainer width="100%" height="100%">
                    <LineChart data={performanceData}>
                      <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))" />
                      <XAxis dataKey="date" stroke="hsl(var(--muted-foreground))" />
                      <YAxis domain={[85, 100]} stroke="hsl(var(--muted-foreground))" />
                      <Tooltip 
                        contentStyle={{ 
                          backgroundColor: 'hsl(var(--card))', 
                          border: '1px solid hsl(var(--border))',
                          borderRadius: '8px'
                        }} 
                      />
                      <Line type="monotone" dataKey="stt" stroke="hsl(var(--primary))" strokeWidth={2} name="STT" />
                      <Line type="monotone" dataKey="tts" stroke="hsl(var(--accent))" strokeWidth={2} name="TTS" />
                      <Line type="monotone" dataKey="qa" stroke="hsl(var(--info))" strokeWidth={2} name="Q&A" />
                      <Line type="monotone" dataKey="response" stroke="hsl(var(--success))" strokeWidth={2} name="응답시간" />
                    </LineChart>
                  </ResponsiveContainer>
                </div>
              </CardContent>
            </Card>
          </div>

          {/* Radar Chart */}
          <Card className="shadow-card border-0">
            <CardHeader>
              <CardTitle className="text-lg">종합 성능</CardTitle>
              <CardDescription>각 지표별 달성도</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="h-72">
                <ResponsiveContainer width="100%" height="100%">
                  <RadarChart data={radarData}>
                    <PolarGrid stroke="hsl(var(--border))" />
                    <PolarAngleAxis dataKey="metric" tick={{ fill: 'hsl(var(--muted-foreground))', fontSize: 11 }} />
                    <PolarRadiusAxis domain={[0, 100]} tick={{ fill: 'hsl(var(--muted-foreground))' }} />
                    <Radar name="성능" dataKey="value" stroke="hsl(var(--primary))" fill="hsl(var(--primary))" fillOpacity={0.3} />
                  </RadarChart>
                </ResponsiveContainer>
              </div>
            </CardContent>
          </Card>
        </div>

        <div className="grid lg:grid-cols-2 gap-6">
          {/* Daily Call Distribution */}
          <Card className="shadow-card border-0">
            <CardHeader>
              <CardTitle className="text-lg">시간대별 통화량</CardTitle>
              <CardDescription>오늘의 시간대별 AI 통화 분포</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="h-64">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart data={dailyCallData}>
                    <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))" />
                    <XAxis dataKey="hour" stroke="hsl(var(--muted-foreground))" />
                    <YAxis stroke="hsl(var(--muted-foreground))" />
                    <Tooltip 
                      contentStyle={{ 
                        backgroundColor: 'hsl(var(--card))', 
                        border: '1px solid hsl(var(--border))',
                        borderRadius: '8px'
                      }} 
                    />
                    <Bar dataKey="calls" fill="hsl(var(--primary))" radius={[4, 4, 0, 0]} name="통화 수" />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            </CardContent>
          </Card>

          {/* Recent Evaluations */}
          <Card className="shadow-card border-0">
            <CardHeader>
              <CardTitle className="text-lg">일별 평가 현황</CardTitle>
              <CardDescription>최근 일별 AI 평가 결과</CardDescription>
            </CardHeader>
            <CardContent>
              <div className="space-y-3">
                {recentEvaluations.map((evaluation) => (
                  <div 
                    key={evaluation.id}
                    className="flex items-center justify-between p-4 rounded-xl bg-secondary/30"
                  >
                    <div>
                      <p className="font-medium">{evaluation.date}</p>
                      <p className="text-sm text-muted-foreground">총 {evaluation.totalCalls}건 통화</p>
                    </div>
                    <div className="text-right">
                      <div className="flex items-center gap-2">
                        <span className="text-lg font-bold">{evaluation.avgScore}%</span>
                        <Badge variant={evaluation.issues > 5 ? "destructive" : evaluation.issues > 3 ? "outline" : "secondary"}>
                          이슈 {evaluation.issues}건
                        </Badge>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            </CardContent>
          </Card>
        </div>
      </div>
    </DashboardLayout>
  );
};

export default AIStats;
