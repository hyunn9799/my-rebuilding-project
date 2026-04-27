import { useNavigate } from "react-router-dom";
import { useEffect } from "react";
import { Button } from "@/components/ui/button";
import {
  Heart,
  Shield,
  Phone,
  Users,
  BarChart3,
  MessageSquare,
  ChevronRight,
  ArrowRight,
  CheckCircle2
} from "lucide-react";
import { useAuth, getRoleHomePath } from "@/contexts/AuthContext";

const features = [
  {
    icon: <Phone className="w-6 h-6" />,
    title: "AI 안부 전화",
    description: "매일 정해진 시간에 어르신께 자동으로 안부 전화를 드립니다.",
  },
  {
    icon: <Heart className="w-6 h-6" />,
    title: "감정 분석",
    description: "통화 내용을 분석하여 어르신의 감정 상태를 파악합니다.",
  },
  {
    icon: <Shield className="w-6 h-6" />,
    title: "긴급 알림",
    description: "위험 신호 감지 시 보호자와 상담사에게 즉시 알립니다.",
  },
  {
    icon: <BarChart3 className="w-6 h-6" />,
    title: "상태 리포트",
    description: "어르신의 건강과 감정 상태를 한눈에 확인할 수 있습니다.",
  },
];

const benefits = [
  "하루 1회 자동 안부 전화",
  "실시간 감정 상태 모니터링",
  "보호자 앱으로 언제든 확인",
  "전문 상담사 1:1 상담 연결",
  "정부 복지 서비스 안내",
  "긴급 상황 즉시 대응",
];

const Index = () => {
  const navigate = useNavigate();
  const { isLoggedIn, role, isLoading } = useAuth();

  // 역할에 따른 대시보드 경로
  const dashboardPath = getRoleHomePath(role);

  // 로그인 상태면 대시보드로 자동 리다이렉트
  useEffect(() => {
    if (!isLoading && isLoggedIn && role) {
      navigate(dashboardPath, { replace: true });
    }
  }, [isLoading, isLoggedIn, role, dashboardPath, navigate]);

  // 로딩 중일 때는 기본 헤더 표시
  if (isLoading) {
    return (
      <div className="min-h-screen bg-gradient-hero">
        <nav className="fixed top-0 left-0 right-0 z-50 bg-background/80 backdrop-blur-md border-b border-border">
          <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
            <div className="flex items-center justify-between h-16">
              <div className="flex items-center gap-3">
                <div className="w-9 h-9 rounded-xl bg-gradient-primary flex items-center justify-center">
                  <Heart className="w-5 h-5 text-primary-foreground" />
                </div>
                <span className="text-xl font-bold text-foreground">마음돌봄</span>
              </div>
            </div>
          </div>
        </nav>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-hero">
      {/* Navigation */}
      <nav className="fixed top-0 left-0 right-0 z-50 bg-background/80 backdrop-blur-md border-b border-border">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-xl bg-gradient-primary flex items-center justify-center">
                <Heart className="w-5 h-5 text-primary-foreground" />
              </div>
              <span className="text-xl font-bold text-foreground">마음돌봄</span>
            </div>
            <div className="flex items-center gap-3">
              {isLoggedIn ? (
                <>
                  <Button variant="ghost" onClick={() => navigate(dashboardPath)}>
                    대시보드
                  </Button>
                  <Button variant="hero" onClick={() => navigate(dashboardPath)}>
                    내 페이지로 이동
                  </Button>
                </>
              ) : (
                <>
                  <Button variant="ghost" onClick={() => navigate("/login")}>
                    로그인
                  </Button>
                  <Button variant="hero" onClick={() => navigate("/login")}>
                    시작하기
                  </Button>
                </>
              )}
            </div>
          </div>
        </div>
      </nav>

      {/* Hero Section */}
      <section className="pt-32 pb-20 px-4 sm:px-6 lg:px-8">
        <div className="max-w-7xl mx-auto">
          <div className="text-center max-w-3xl mx-auto">
            <div className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-primary/10 text-primary text-sm font-medium mb-6 animate-fade-in">
              <Shield className="w-4 h-4" />
              국가 공공 복지 서비스
            </div>
            <h1 className="text-4xl sm:text-5xl lg:text-6xl font-bold text-foreground leading-tight animate-fade-in">
              어르신의 <span className="text-gradient-primary">마음을 돌보는</span>
              <br />AI 안부 서비스
            </h1>
            <p className="mt-6 text-lg text-muted-foreground max-w-2xl mx-auto animate-fade-in">
              매일 따뜻한 안부 전화로 어르신의 건강과 감정을 살피고,
              보호자와 상담사에게 실시간으로 전달합니다.
            </p>
            <div className="mt-10 flex flex-col sm:flex-row items-center justify-center gap-4 animate-fade-in">
              <Button
                variant="hero"
                size="xl"
                onClick={() => navigate("/login")}
                className="w-full sm:w-auto"
              >
                서비스 이용하기
                <ArrowRight className="w-5 h-5" />
              </Button>
              <Button
                variant="outline"
                size="xl"
                className="w-full sm:w-auto"
              >
                자세히 알아보기
              </Button>
            </div>
          </div>

          {/* Stats */}
          <div className="mt-20 grid grid-cols-2 lg:grid-cols-4 gap-6">
            {[
              { value: "50,000+", label: "서비스 이용 어르신" },
              { value: "1,200+", label: "전문 상담사" },
              { value: "99.2%", label: "통화 성공률" },
              { value: "24/7", label: "긴급 대응 체계" },
            ].map((stat, index) => (
              <div
                key={index}
                className="p-6 rounded-2xl bg-card shadow-card text-center animate-fade-in"
                style={{ animationDelay: `${index * 0.1}s` }}
              >
                <p className="text-3xl font-bold text-primary">{stat.value}</p>
                <p className="mt-1 text-sm text-muted-foreground">{stat.label}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section className="py-20 px-4 sm:px-6 lg:px-8 bg-card">
        <div className="max-w-7xl mx-auto">
          <div className="text-center mb-16">
            <h2 className="text-3xl font-bold text-foreground">주요 기능</h2>
            <p className="mt-4 text-muted-foreground">
              AI 기술로 어르신의 안전과 건강을 지킵니다
            </p>
          </div>
          <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6">
            {features.map((feature, index) => (
              <div
                key={index}
                className="p-6 rounded-2xl bg-background shadow-card hover:shadow-elevated transition-all duration-300 group"
              >
                <div className="w-12 h-12 rounded-xl bg-gradient-primary flex items-center justify-center text-primary-foreground mb-4 group-hover:scale-110 transition-transform">
                  {feature.icon}
                </div>
                <h3 className="text-lg font-semibold text-foreground mb-2">
                  {feature.title}
                </h3>
                <p className="text-muted-foreground text-sm">
                  {feature.description}
                </p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Benefits Section */}
      <section className="py-20 px-4 sm:px-6 lg:px-8">
        <div className="max-w-7xl mx-auto">
          <div className="grid lg:grid-cols-2 gap-12 items-center">
            <div>
              <h2 className="text-3xl font-bold text-foreground mb-6">
                보호자를 위한
                <br />
                <span className="text-gradient-primary">안심 서비스</span>
              </h2>
              <p className="text-muted-foreground mb-8">
                멀리 떨어져 있어도 부모님의 안부를 매일 확인할 수 있습니다.
                전문 상담사와 AI가 함께 어르신을 돌봅니다.
              </p>
              <div className="grid sm:grid-cols-2 gap-4">
                {benefits.map((benefit, index) => (
                  <div key={index} className="flex items-center gap-3">
                    <CheckCircle2 className="w-5 h-5 text-success flex-shrink-0" />
                    <span className="text-foreground">{benefit}</span>
                  </div>
                ))}
              </div>
              <Button
                variant="hero"
                size="lg"
                className="mt-8"
                onClick={() => navigate("/login")}
              >
                지금 시작하기
                <ChevronRight className="w-5 h-5" />
              </Button>
            </div>
            <div className="relative">
              <div className="absolute inset-0 bg-gradient-primary opacity-10 rounded-3xl blur-3xl" />
              <div className="relative bg-card rounded-3xl p-8 shadow-elevated">
                <div className="flex items-center gap-4 mb-6">
                  <div className="w-14 h-14 rounded-full bg-accent/10 flex items-center justify-center">
                    <Heart className="w-7 h-7 text-accent" />
                  </div>
                  <div>
                    <h3 className="font-semibold text-foreground">김순자 어르신</h3>
                    <p className="text-sm text-muted-foreground">오늘 10:30 통화 완료</p>
                  </div>
                </div>
                <div className="space-y-4">
                  <div className="p-4 rounded-xl bg-success/10">
                    <p className="text-sm font-medium text-success">감정 상태: 좋음 😊</p>
                    <p className="text-xs text-muted-foreground mt-1">
                      오늘 아침 산책을 다녀오셨다고 하시며 기분이 좋으신 것 같습니다.
                    </p>
                  </div>
                  <div className="p-4 rounded-xl bg-secondary">
                    <p className="text-sm font-medium text-secondary-foreground">식사 여부</p>
                    <p className="text-xs text-muted-foreground mt-1">
                      아침: ✓ 완료 | 점심: 예정
                    </p>
                  </div>
                  <div className="p-4 rounded-xl bg-primary/10">
                    <p className="text-sm font-medium text-primary">특이사항 없음</p>
                    <p className="text-xs text-muted-foreground mt-1">
                      건강 상태 양호, 다음 통화: 내일 10:30
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-20 px-4 sm:px-6 lg:px-8">
        <div className="max-w-4xl mx-auto">
          <div className="bg-gradient-primary rounded-3xl p-8 sm:p-12 text-center text-primary-foreground">
            <h2 className="text-3xl font-bold mb-4">
              지금 바로 시작하세요
            </h2>
            <p className="text-primary-foreground/80 mb-8 max-w-xl mx-auto">
              무료로 서비스를 체험해보세요. 어르신의 안전과 건강을 함께 지켜드립니다.
            </p>
            <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
              <Button
                variant="secondary"
                size="xl"
                onClick={() => navigate("/login")}
                className="w-full sm:w-auto"
              >
                보호자 가입하기
                <Users className="w-5 h-5" />
              </Button>
              <Button
                variant="outline"
                size="xl"
                className="w-full sm:w-auto border-primary-foreground/30 text-primary-foreground hover:bg-primary-foreground/10"
                onClick={() => navigate("/login")}
              >
                상담사 로그인
              </Button>
            </div>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="py-12 px-4 sm:px-6 lg:px-8 border-t border-border bg-card">
        <div className="max-w-7xl mx-auto">
          <div className="flex flex-col md:flex-row items-center justify-between gap-6">
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-xl bg-gradient-primary flex items-center justify-center">
                <Heart className="w-5 h-5 text-primary-foreground" />
              </div>
              <div>
                <span className="font-bold text-foreground">마음돌봄</span>
                <p className="text-xs text-muted-foreground">국가 공공 복지 서비스</p>
              </div>
            </div>
            <div className="flex items-center gap-6 text-sm text-muted-foreground">
              <a href="#" className="hover:text-foreground transition-colors">이용약관</a>
              <a href="#" className="hover:text-foreground transition-colors">개인정보처리방침</a>
              <a href="#" className="hover:text-foreground transition-colors">고객센터</a>
            </div>
          </div>
          <div className="mt-8 pt-8 border-t border-border text-center text-sm text-muted-foreground">
            © 2024 마음돌봄. All rights reserved.
          </div>
        </div>
      </footer>
    </div>
  );
};

export default Index;
