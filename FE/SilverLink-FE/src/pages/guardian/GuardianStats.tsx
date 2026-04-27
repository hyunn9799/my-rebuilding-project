import { useState } from "react";
import {
  TrendingUp,
  Phone,
  Utensils,
  Moon,
  BarChart3,
  Activity
} from "lucide-react";
import { useAuth } from "@/contexts/AuthContext";
import { guardianNavItems } from "@/config/guardianNavItems";
import DashboardLayout from "@/components/layout/DashboardLayout";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  LineChart,
  Line,
  AreaChart,
  Area,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
} from "recharts";

// 빈 상태 컴포넌트
const EmptyState = ({ icon: Icon, message }: { icon: React.ComponentType<{ className?: string }>, message: string }) => (
  <div className="flex flex-col items-center justify-center h-64 text-muted-foreground">
    <Icon className="w-12 h-12 mb-4 opacity-30" />
    <p className="text-center whitespace-pre-line">{message}</p>
  </div>
);

// 커스텀 툴팁
const CustomTooltip = ({ active, payload, label }: any) => {
  if (active && payload && payload.length) {
    return (
      <div className="bg-card p-3 rounded-lg shadow-elevated border border-border">
        <p className="font-medium text-foreground mb-2">{label}</p>
        {payload.map((entry: any, index: number) => (
          <p key={index} className="text-sm" style={{ color: entry.color }}>
            {entry.name}: {entry.value}{entry.unit || ''}
          </p>
        ))}
      </div>
    );
  }
  return null;
};

// 타입 정의
interface EmotionData {
  day?: string;
  month?: string;
  good: number;
  neutral: number;
  bad: number;
  score: number;
  calls?: number;
}

interface EmotionDistribution {
  name: string;
  value: number;
  color: string;
}

interface ActivitiesData {
  day: string;
  meals: number;
  sleep: number;
  exercise: number;
}

interface StatsData {
  avgCallDuration: number | null;
  totalCalls: number | null;
  avgMeals: number | null;
  avgSleep: number | null;
  callDurationChange: number | null;
}

const GuardianStats = () => {
  const { user } = useAuth();
  const [period, setPeriod] = useState("weekly");

  // TODO: API에서 데이터를 받아오면 이 상태들을 업데이트
  // 현재는 빈 배열/null로 초기화 (데이터 없음 상태)
  const [weeklyEmotionData] = useState<EmotionData[]>([]);
  const [monthlyEmotionData] = useState<EmotionData[]>([]);
  const [emotionDistribution] = useState<EmotionDistribution[]>([]);
  const [weeklyActivitiesData] = useState<ActivitiesData[]>([]);
  const [statsData] = useState<StatsData>({
    avgCallDuration: null,
    totalCalls: null,
    avgMeals: null,
    avgSleep: null,
    callDurationChange: null,
  });

  const currentData = period === "weekly" ? weeklyEmotionData : monthlyEmotionData;
  const hasEmotionData = currentData.length > 0;
  const hasDistributionData = emotionDistribution.length > 0;
  const hasActivitiesData = weeklyActivitiesData.length > 0;

  return (
    <DashboardLayout
      role="guardian"
      userName={user?.name || "보호자"}
      navItems={guardianNavItems}
    >
      <div className="space-y-6">
        {/* Page Header */}
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-foreground">통계</h1>
            <p className="text-muted-foreground mt-1">부모님의 감정 및 생활 패턴을 분석합니다</p>
          </div>
          <div className="flex items-center gap-2">
            <Button
              variant={period === "weekly" ? "default" : "outline"}
              size="sm"
              onClick={() => setPeriod("weekly")}
            >
              주간
            </Button>
            <Button
              variant={period === "monthly" ? "default" : "outline"}
              size="sm"
              onClick={() => setPeriod("monthly")}
            >
              월간
            </Button>
          </div>
        </div>

        {/* Summary Cards */}
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          <Card className="shadow-card border-0">
            <CardContent className="p-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-muted-foreground">평균 통화시간</p>
                  {statsData.avgCallDuration !== null ? (
                    <>
                      <p className="text-3xl font-bold text-foreground mt-1">
                        {statsData.avgCallDuration}<span className="text-lg">분</span>
                      </p>
                      {statsData.callDurationChange !== null && (
                        <div className={`flex items-center gap-1 mt-1 text-sm ${statsData.callDurationChange >= 0 ? 'text-success' : 'text-destructive'}`}>
                          <TrendingUp className="w-4 h-4" />
                          <span>{Math.abs(statsData.callDurationChange)}분 {statsData.callDurationChange >= 0 ? '증가' : '감소'}</span>
                        </div>
                      )}
                    </>
                  ) : (
                    <>
                      <p className="text-3xl font-bold text-muted-foreground/50 mt-1">-</p>
                      <p className="text-sm text-muted-foreground mt-1">데이터 없음</p>
                    </>
                  )}
                </div>
                <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${statsData.avgCallDuration !== null ? 'bg-primary/10' : 'bg-muted'}`}>
                  <Phone className={`w-6 h-6 ${statsData.avgCallDuration !== null ? 'text-primary' : 'text-muted-foreground/50'}`} />
                </div>
              </div>
            </CardContent>
          </Card>

          <Card className="shadow-card border-0">
            <CardContent className="p-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-muted-foreground">총 통화 횟수</p>
                  {statsData.totalCalls !== null ? (
                    <>
                      <p className="text-3xl font-bold text-foreground mt-1">{statsData.totalCalls}</p>
                      <p className="text-sm text-muted-foreground mt-1">
                        {period === "weekly" ? "이번 주" : "이번 달"}
                      </p>
                    </>
                  ) : (
                    <>
                      <p className="text-3xl font-bold text-muted-foreground/50 mt-1">-</p>
                      <p className="text-sm text-muted-foreground mt-1">데이터 없음</p>
                    </>
                  )}
                </div>
                <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${statsData.totalCalls !== null ? 'bg-primary/10' : 'bg-muted'}`}>
                  <Phone className={`w-6 h-6 ${statsData.totalCalls !== null ? 'text-primary' : 'text-muted-foreground/50'}`} />
                </div>
              </div>
            </CardContent>
          </Card>

          <Card className="shadow-card border-0">
            <CardContent className="p-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-muted-foreground">평균 식사 횟수</p>
                  {statsData.avgMeals !== null ? (
                    <>
                      <p className="text-3xl font-bold text-foreground mt-1">{statsData.avgMeals}</p>
                      <p className="text-sm text-success mt-1">일 3회 권장</p>
                    </>
                  ) : (
                    <>
                      <p className="text-3xl font-bold text-muted-foreground/50 mt-1">-</p>
                      <p className="text-sm text-muted-foreground mt-1">데이터 없음</p>
                    </>
                  )}
                </div>
                <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${statsData.avgMeals !== null ? 'bg-accent/10' : 'bg-muted'}`}>
                  <Utensils className={`w-6 h-6 ${statsData.avgMeals !== null ? 'text-accent' : 'text-muted-foreground/50'}`} />
                </div>
              </div>
            </CardContent>
          </Card>

          <Card className="shadow-card border-0">
            <CardContent className="p-6">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm text-muted-foreground">평균 수면 시간</p>
                  {statsData.avgSleep !== null ? (
                    <>
                      <p className="text-3xl font-bold text-foreground mt-1">{statsData.avgSleep}<span className="text-lg">h</span></p>
                      <p className="text-sm text-warning mt-1">7-8시간 권장</p>
                    </>
                  ) : (
                    <>
                      <p className="text-3xl font-bold text-muted-foreground/50 mt-1">-</p>
                      <p className="text-sm text-muted-foreground mt-1">데이터 없음</p>
                    </>
                  )}
                </div>
                <div className={`w-12 h-12 rounded-xl flex items-center justify-center ${statsData.avgSleep !== null ? 'bg-info/10' : 'bg-muted'}`}>
                  <Moon className={`w-6 h-6 ${statsData.avgSleep !== null ? 'text-info' : 'text-muted-foreground/50'}`} />
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        <div className="grid lg:grid-cols-3 gap-6">
          {/* Emotion Trend Chart */}
          <div className="lg:col-span-2">
            <Card className="shadow-card border-0 h-full">
              <CardHeader>
                <CardTitle className="text-lg">감정 추이</CardTitle>
                <CardDescription>
                  {period === "weekly" ? "이번 주" : "최근 6개월"} 감정 점수 변화
                </CardDescription>
              </CardHeader>
              <CardContent>
                {hasEmotionData ? (
                  <div className="h-80">
                    <ResponsiveContainer width="100%" height="100%">
                      <AreaChart
                        data={currentData}
                        margin={{ top: 10, right: 10, left: -20, bottom: 0 }}
                      >
                        <defs>
                          <linearGradient id="colorScore" x1="0" y1="0" x2="0" y2="1">
                            <stop offset="5%" stopColor="hsl(172, 50%, 38%)" stopOpacity={0.3} />
                            <stop offset="95%" stopColor="hsl(172, 50%, 38%)" stopOpacity={0} />
                          </linearGradient>
                        </defs>
                        <CartesianGrid strokeDasharray="3 3" stroke="hsl(220, 15%, 88%)" vertical={false} />
                        <XAxis
                          dataKey={period === "weekly" ? "day" : "month"}
                          axisLine={false}
                          tickLine={false}
                          tick={{ fill: 'hsl(220, 10%, 50%)', fontSize: 12 }}
                        />
                        <YAxis
                          domain={[0, 100]}
                          axisLine={false}
                          tickLine={false}
                          tick={{ fill: 'hsl(220, 10%, 50%)', fontSize: 12 }}
                        />
                        <Tooltip content={<CustomTooltip />} />
                        <Area
                          type="monotone"
                          dataKey="score"
                          stroke="hsl(172, 50%, 38%)"
                          strokeWidth={3}
                          fillOpacity={1}
                          fill="url(#colorScore)"
                          name="감정 점수"
                        />
                      </AreaChart>
                    </ResponsiveContainer>
                  </div>
                ) : (
                  <EmptyState
                    icon={TrendingUp}
                    message={`감정 분석 데이터가 없습니다.\n통화 기록이 쌓이면 감정 추이를 확인할 수 있습니다.`}
                  />
                )}
              </CardContent>
            </Card>
          </div>

          {/* Emotion Distribution */}
          <div>
            <Card className="shadow-card border-0 h-full">
              <CardHeader>
                <CardTitle className="text-lg">감정 분포</CardTitle>
                <CardDescription>
                  {period === "weekly" ? "이번 주" : "이번 달"} 감정 상태 비율
                </CardDescription>
              </CardHeader>
              <CardContent>
                {hasDistributionData ? (
                  <>
                    <div className="h-64">
                      <ResponsiveContainer width="100%" height="100%">
                        <PieChart>
                          <Pie
                            data={emotionDistribution}
                            cx="50%"
                            cy="50%"
                            innerRadius={60}
                            outerRadius={90}
                            paddingAngle={3}
                            dataKey="value"
                          >
                            {emotionDistribution.map((entry, index) => (
                              <Cell key={`cell-${index}`} fill={entry.color} />
                            ))}
                          </Pie>
                          <Tooltip content={<CustomTooltip />} />
                        </PieChart>
                      </ResponsiveContainer>
                    </div>
                    <div className="flex justify-center gap-6 mt-4">
                      {emotionDistribution.map((item, index) => (
                        <div key={index} className="flex items-center gap-2">
                          <div className="w-3 h-3 rounded-full" style={{ backgroundColor: item.color }} />
                          <span className="text-sm text-muted-foreground">{item.name}</span>
                          <span className="text-sm font-medium">{item.value}%</span>
                        </div>
                      ))}
                    </div>
                  </>
                ) : (
                  <EmptyState
                    icon={BarChart3}
                    message="감정 분포 데이터가 없습니다."
                  />
                )}
              </CardContent>
            </Card>
          </div>
        </div>

        {/* Emotion Detail Bar Chart */}
        <Card className="shadow-card border-0">
          <CardHeader>
            <CardTitle className="text-lg">일별 감정 상세</CardTitle>
            <CardDescription>
              {period === "weekly" ? "요일별" : "월별"} 감정 상태 분포
            </CardDescription>
          </CardHeader>
          <CardContent>
            {hasEmotionData ? (
              <div className="h-72">
                <ResponsiveContainer width="100%" height="100%">
                  <BarChart
                    data={currentData}
                    margin={{ top: 10, right: 10, left: -20, bottom: 0 }}
                  >
                    <CartesianGrid strokeDasharray="3 3" stroke="hsl(220, 15%, 88%)" vertical={false} />
                    <XAxis
                      dataKey={period === "weekly" ? "day" : "month"}
                      axisLine={false}
                      tickLine={false}
                      tick={{ fill: 'hsl(220, 10%, 50%)', fontSize: 12 }}
                    />
                    <YAxis
                      axisLine={false}
                      tickLine={false}
                      tick={{ fill: 'hsl(220, 10%, 50%)', fontSize: 12 }}
                    />
                    <Tooltip content={<CustomTooltip />} />
                    <Bar dataKey="good" stackId="a" fill="hsl(145, 60%, 42%)" name="좋음" radius={[0, 0, 0, 0]} />
                    <Bar dataKey="neutral" stackId="a" fill="hsl(38, 92%, 55%)" name="보통" radius={[0, 0, 0, 0]} />
                    <Bar dataKey="bad" stackId="a" fill="hsl(0, 72%, 55%)" name="주의" radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              </div>
            ) : (
              <EmptyState
                icon={BarChart3}
                message="일별 감정 데이터가 없습니다."
              />
            )}
          </CardContent>
        </Card>

        {/* Activities Chart */}
        <Card className="shadow-card border-0">
          <CardHeader>
            <CardTitle className="text-lg">생활 패턴 분석</CardTitle>
            <CardDescription>
              식사, 수면, 운동 패턴을 확인하세요
            </CardDescription>
          </CardHeader>
          <CardContent>
            <Tabs defaultValue="meals">
              <TabsList className="mb-4">
                <TabsTrigger value="meals" className="gap-2">
                  <Utensils className="w-4 h-4" />
                  식사
                </TabsTrigger>
                <TabsTrigger value="sleep" className="gap-2">
                  <Moon className="w-4 h-4" />
                  수면
                </TabsTrigger>
                <TabsTrigger value="exercise" className="gap-2">
                  <Activity className="w-4 h-4" />
                  운동
                </TabsTrigger>
              </TabsList>

              <TabsContent value="meals">
                {hasActivitiesData ? (
                  <div className="h-64">
                    <ResponsiveContainer width="100%" height="100%">
                      <BarChart
                        data={weeklyActivitiesData}
                        margin={{ top: 10, right: 10, left: -20, bottom: 0 }}
                      >
                        <CartesianGrid strokeDasharray="3 3" stroke="hsl(220, 15%, 88%)" vertical={false} />
                        <XAxis
                          dataKey="day"
                          axisLine={false}
                          tickLine={false}
                          tick={{ fill: 'hsl(220, 10%, 50%)', fontSize: 12 }}
                        />
                        <YAxis
                          domain={[0, 3]}
                          axisLine={false}
                          tickLine={false}
                          tick={{ fill: 'hsl(220, 10%, 50%)', fontSize: 12 }}
                        />
                        <Tooltip content={<CustomTooltip />} />
                        <Bar dataKey="meals" fill="hsl(16, 80%, 60%)" name="식사 횟수" radius={[4, 4, 0, 0]} />
                      </BarChart>
                    </ResponsiveContainer>
                  </div>
                ) : (
                  <EmptyState
                    icon={Utensils}
                    message="식사 패턴 데이터가 없습니다."
                  />
                )}
              </TabsContent>

              <TabsContent value="sleep">
                {hasActivitiesData ? (
                  <div className="h-64">
                    <ResponsiveContainer width="100%" height="100%">
                      <LineChart
                        data={weeklyActivitiesData}
                        margin={{ top: 10, right: 10, left: -20, bottom: 0 }}
                      >
                        <CartesianGrid strokeDasharray="3 3" stroke="hsl(220, 15%, 88%)" vertical={false} />
                        <XAxis
                          dataKey="day"
                          axisLine={false}
                          tickLine={false}
                          tick={{ fill: 'hsl(220, 10%, 50%)', fontSize: 12 }}
                        />
                        <YAxis
                          domain={[0, 10]}
                          axisLine={false}
                          tickLine={false}
                          tick={{ fill: 'hsl(220, 10%, 50%)', fontSize: 12 }}
                        />
                        <Tooltip content={<CustomTooltip />} />
                        <Line
                          type="monotone"
                          dataKey="sleep"
                          stroke="hsl(210, 80%, 55%)"
                          strokeWidth={3}
                          dot={{ fill: "hsl(210, 80%, 55%)", strokeWidth: 0 }}
                          name="수면 시간"
                          unit="시간"
                        />
                      </LineChart>
                    </ResponsiveContainer>
                  </div>
                ) : (
                  <EmptyState
                    icon={Moon}
                    message="수면 패턴 데이터가 없습니다."
                  />
                )}
              </TabsContent>

              <TabsContent value="exercise">
                {hasActivitiesData ? (
                  <div className="h-64">
                    <ResponsiveContainer width="100%" height="100%">
                      <BarChart
                        data={weeklyActivitiesData}
                        margin={{ top: 10, right: 10, left: -20, bottom: 0 }}
                      >
                        <CartesianGrid strokeDasharray="3 3" stroke="hsl(220, 15%, 88%)" vertical={false} />
                        <XAxis
                          dataKey="day"
                          axisLine={false}
                          tickLine={false}
                          tick={{ fill: 'hsl(220, 10%, 50%)', fontSize: 12 }}
                        />
                        <YAxis
                          domain={[0, 2]}
                          axisLine={false}
                          tickLine={false}
                          tick={{ fill: 'hsl(220, 10%, 50%)', fontSize: 12 }}
                        />
                        <Tooltip content={<CustomTooltip />} />
                        <Bar dataKey="exercise" fill="hsl(145, 60%, 42%)" name="운동 횟수" radius={[4, 4, 0, 0]} />
                      </BarChart>
                    </ResponsiveContainer>
                  </div>
                ) : (
                  <EmptyState
                    icon={Activity}
                    message="운동 패턴 데이터가 없습니다."
                  />
                )}
              </TabsContent>
            </Tabs>
          </CardContent>
        </Card>
      </div>
    </DashboardLayout>
  );
};

export default GuardianStats;
