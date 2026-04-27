import { useState, useEffect } from "react";
import {
  Heart,
  Search,
  ExternalLink,
  ChevronRight,
  ChevronLeft,
  Users,
  Loader2,
  RefreshCw,
  MapPin,
  CheckCircle2,
  Info
} from "lucide-react";
import { toast } from "sonner";
import { guardianNavItems } from "@/config/guardianNavItems";
import DashboardLayout from "@/components/layout/DashboardLayout";
import { Card, CardContent } from "@/components/ui/card";
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
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Separator } from "@/components/ui/separator";
import { ScrollArea } from "@/components/ui/scroll-area";
import welfareApi from "@/api/welfare";
import usersApi from "@/api/users";
import guardiansApi from "@/api/guardians";
import { WelfareListResponse, WelfareDetailResponse, MyProfileResponse } from "@/types/api";

// 중앙부처 키워드 목록
const CENTRAL_DEPARTMENTS = [
  '보건복지부', '고용노동부', '국토교통부', '과학기술정보통신부',
  '교육부', '문화체육관광부', '농림축산식품부', '산업통상자원부',
  '환경부', '국방부', '법무부', '행정안전부', '외교부', '통일부',
  '기획재정부', '여성가족부', '국가보훈처', '해양수산부', '중소벤처기업부'
];

// 소관기관명을 기준으로 중앙부처 여부 판별
const isCentralDepartment = (jurMnofNm: string | undefined): boolean => {
  if (!jurMnofNm) return false;
  return CENTRAL_DEPARTMENTS.some(dept => jurMnofNm.includes(dept));
};

// 서비스 출처 라벨 반환 (source 필드 + jurMnofNm 검사)
const getSourceLabel = (service: WelfareListResponse): string => {
  if (service.source === 'CENTRAL' || isCentralDepartment(service.jurMnofNm)) {
    return '중앙부처';
  }
  return '지자체';
};

const GuardianWelfare = () => {
  const [searchQuery, setSearchQuery] = useState("");

  const [isLoading, setIsLoading] = useState(true);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [services, setServices] = useState<WelfareListResponse[]>([]);
  const [selectedService, setSelectedService] = useState<WelfareDetailResponse | null>(null);
  const [userProfile, setUserProfile] = useState<MyProfileResponse | null>(null);
  const [parentName, setParentName] = useState("어르신");

  // Pagination state
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const pageSize = 10;

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      setIsLoading(true);

      // 사용자 프로필 조회
      const profile = await usersApi.getMyProfile();
      setUserProfile(profile);

      // 어르신 정보 조회
      try {
        const elderly = await guardiansApi.getMyElderly();
        if (elderly && elderly.elderlyName) {
          setParentName(elderly.elderlyName);
        }
      } catch (e) {
        // Fail silently or handle error appropriately
      }

      // 복지 서비스 목록 조회
      await fetchWelfareServices();
    } catch (error) {
      console.error('Failed to fetch data:', error);
    } finally {
      setIsLoading(false);
    }
  };

  const fetchWelfareServices = async (page: number = currentPage) => {
    try {
      const params: { keyword?: string; region?: string; page?: number; size?: number } = {
        page,
        size: pageSize,
      };
      if (searchQuery) params.keyword = searchQuery;


      const response = await welfareApi.searchWelfare(params);
      setServices(response.content || []);
      setTotalPages(response.totalPages || 0);
      setTotalElements(response.totalElements || 0);
      setCurrentPage(page);
    } catch (error) {
      console.error('Failed to fetch welfare services:', error);
      toast.error("서비스 목록을 불러오지 못했습니다.", {
        description: "잠시 후 다시 시도해주세요."
      });
      setServices([]);
    }
  };

  const handleSearch = async () => {
    setCurrentPage(0);
    await fetchWelfareServices(0);
  };

  const handlePageChange = async (page: number) => {
    if (page >= 0 && page < totalPages) {
      await fetchWelfareServices(page);
    }
  };

  const handleRefresh = async () => {
    setIsRefreshing(true);
    await fetchWelfareServices();
    setIsRefreshing(false);
  };

  const handleServiceClick = async (serviceId: number) => {
    try {
      const detail = await welfareApi.getWelfareDetail(serviceId);
      setSelectedService(detail);
    } catch (error) {
      console.error('Failed to fetch service detail:', error);
      toast.error("서비스 상세 정보를 불러오지 못했습니다.");
    }
  };

  if (isLoading) {
    return (
      <DashboardLayout role="guardian" userName="로딩중..." navItems={guardianNavItems}>
        <div className="flex items-center justify-center h-64">
          <Loader2 className="w-8 h-8 animate-spin text-primary" />
        </div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout
      role="guardian"
      userName={userProfile?.name || "보호자"}
      navItems={guardianNavItems}
    >
      <div className="space-y-6">
        {/* 페이지 헤더 */}
        <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-foreground">복지 서비스 안내</h1>
            <p className="text-muted-foreground mt-1">
              {parentName}님이 받을 수 있는 정부 복지 서비스를 확인하세요
            </p>
          </div>
          <Button onClick={handleRefresh} variant="outline" disabled={isRefreshing}>
            {isRefreshing ? (
              <Loader2 className="w-4 h-4 mr-2 animate-spin" />
            ) : (
              <RefreshCw className="w-4 h-4 mr-2" />
            )}
            정보 새로고침
          </Button>
        </div>

        {/* 통계 카드 */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <Card className="border-primary/20 bg-primary/5">
            <CardContent className="p-4">
              <div className="flex items-center gap-4">
                <div className="w-12 h-12 rounded-xl bg-primary/10 flex items-center justify-center">
                  <Heart className="w-6 h-6 text-primary" />
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">전체 서비스</p>
                  <p className="text-2xl font-bold text-foreground">{totalElements}개</p>
                </div>
              </div>
            </CardContent>
          </Card>
          <Card className="border-success/20 bg-success/5">
            <CardContent className="p-4">
              <div className="flex items-center gap-4">
                <div className="w-12 h-12 rounded-xl bg-success/10 flex items-center justify-center">
                  <CheckCircle2 className="w-6 h-6 text-success" />
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">현재 페이지</p>
                  <p className="text-2xl font-bold text-foreground">{currentPage + 1} / {totalPages}페이지</p>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* 검색 및 필터 */}
        <Card>
          <CardContent className="p-4">
            <div className="flex flex-col lg:flex-row gap-4">
              <div className="relative flex-1">
                <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                <Input
                  placeholder="서비스명 또는 내용으로 검색..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
                  className="pl-10"
                />
              </div>
              <div className="flex flex-wrap gap-2">

                <Button onClick={handleSearch}>
                  <Search className="w-4 h-4 mr-2" />
                  검색
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>

        {/* 서비스 목록 */}
        <div className="space-y-4">
          {services.length === 0 ? (
            <Card>
              <CardContent className="p-12 text-center">
                <Heart className="w-12 h-12 text-muted-foreground/30 mx-auto mb-4" />
                <p className="text-muted-foreground">
                  검색 조건에 맞는 서비스가 없습니다
                </p>
              </CardContent>
            </Card>
          ) : (
            <>
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
                {services.map((service) => (
                  <Dialog key={service.id}>
                    <DialogTrigger asChild>
                      <Card
                        className="cursor-pointer hover:shadow-md transition-all duration-200 hover:border-primary/30"
                        onClick={() => handleServiceClick(service.id)}
                      >
                        <CardContent className="p-5">
                          <div className="flex items-start justify-between gap-4">
                            <div className="flex-1 min-w-0">
                              <div className="flex items-center gap-2 mb-2">
                                <div className="w-8 h-8 rounded-lg bg-primary/10 flex items-center justify-center shrink-0">
                                  <Heart className="w-4 h-4 text-primary" />
                                </div>
                                <Badge variant="outline">{getSourceLabel(service)}</Badge>
                              </div>
                              <h3 className="font-semibold text-foreground mb-1">{service.servNm}</h3>
                              <p className="text-sm text-muted-foreground line-clamp-2 mb-3">
                                {service.servDgst}
                              </p>
                              <div className="flex flex-wrap items-center gap-3 text-xs text-muted-foreground">
                                <span className="flex items-center gap-1">
                                  <Users className="w-3 h-3" />
                                  {getSourceLabel(service)}
                                </span>
                                <span className="flex items-center gap-1">
                                  <MapPin className="w-3 h-3" />
                                  {service.jurMnofNm || '전국'}
                                </span>
                              </div>
                            </div>
                            <ChevronRight className="w-5 h-5 text-muted-foreground shrink-0" />
                          </div>
                        </CardContent>
                      </Card>
                    </DialogTrigger>
                    <DialogContent className="max-w-2xl max-h-[90vh]">
                      <DialogHeader>
                        <div className="flex items-center gap-3">
                          <div className="w-10 h-10 rounded-xl bg-primary/10 flex items-center justify-center">
                            <Heart className="w-5 h-5 text-primary" />
                          </div>
                          <div>
                            <DialogTitle className="text-xl">{service.servNm}</DialogTitle>
                            <DialogDescription>{getSourceLabel(service)} 서비스</DialogDescription>
                          </div>
                        </div>
                      </DialogHeader>
                      <ScrollArea className="max-h-[60vh] pr-4">
                        {selectedService ? (
                          <div className="space-y-6 py-4">
                            <div>
                              <h4 className="font-medium text-foreground mb-2">서비스 안내</h4>
                              <p className="text-muted-foreground">{selectedService.alwServCn || selectedService.servDgst}</p>
                            </div>

                            <Separator />

                            <div>
                              <h4 className="font-medium text-foreground mb-2">지원 대상</h4>
                              <p className="text-muted-foreground">{selectedService.targetDtlCn || '해당 정보 없음'}</p>
                            </div>

                            <div>
                              <h4 className="font-medium text-foreground mb-2">선정 기준</h4>
                              <p className="text-muted-foreground">{selectedService.slctCritCn || '관할 주민센터 방문 또는 온라인 신청'}</p>
                            </div>

                            <Separator />

                            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                              <div className="p-4 rounded-xl bg-muted/50">
                                <p className="text-sm text-muted-foreground mb-1">문의처</p>
                                <p className="font-medium text-foreground">{selectedService.rprsCtadr || '129 (정부민원안내)'}</p>
                              </div>
                              <div className="p-4 rounded-xl bg-muted/50">
                                <p className="text-sm text-muted-foreground mb-1">소관기관</p>
                                <p className="font-medium text-foreground">{selectedService.jurMnofNm || '미상'}</p>
                              </div>
                            </div>

                            {selectedService.servDtlLink && (
                              <Button asChild className="w-full">
                                <a href={selectedService.servDtlLink} target="_blank" rel="noopener noreferrer">
                                  <ExternalLink className="w-4 h-4 mr-2" />
                                  상세정보 및 신청 바로가기
                                </a>
                              </Button>
                            )}
                          </div>
                        ) : (
                          <div className="flex items-center justify-center py-8">
                            <Loader2 className="w-6 h-6 animate-spin text-muted-foreground" />
                          </div>
                        )}
                      </ScrollArea>
                    </DialogContent>
                  </Dialog>
                ))}
              </div>

              {/* 페이지네이션 UI */}
              {totalPages > 1 && (
                <div className="flex items-center justify-center gap-2 mt-6">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handlePageChange(currentPage - 1)}
                    disabled={currentPage === 0}
                  >
                    <ChevronLeft className="w-4 h-4 mr-1" />
                    이전
                  </Button>

                  <div className="flex items-center gap-1">
                    {Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
                      let pageNum;
                      if (totalPages <= 5) {
                        pageNum = i;
                      } else if (currentPage < 3) {
                        pageNum = i;
                      } else if (currentPage > totalPages - 4) {
                        pageNum = totalPages - 5 + i;
                      } else {
                        pageNum = currentPage - 2 + i;
                      }
                      return (
                        <Button
                          key={pageNum}
                          variant={currentPage === pageNum ? "default" : "outline"}
                          size="sm"
                          onClick={() => handlePageChange(pageNum)}
                          className="w-10"
                        >
                          {pageNum + 1}
                        </Button>
                      );
                    })}
                  </div>

                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handlePageChange(currentPage + 1)}
                    disabled={currentPage >= totalPages - 1}
                  >
                    다음
                    <ChevronRight className="w-4 h-4 ml-1" />
                  </Button>
                </div>
              )}
            </>
          )}
        </div>

        {/* 안내 메시지 */}
        <Card className="border-info/20 bg-info/5">
          <CardContent className="p-4">
            <div className="flex items-start gap-3">
              <Info className="w-5 h-5 text-info shrink-0 mt-0.5" />
              <div className="text-sm">
                <p className="font-medium text-foreground mb-1">복지 서비스 안내</p>
                <p className="text-muted-foreground">
                  본 정보는 공공데이터포털 API를 통해 제공되며, 실제 수급 자격은 관할 주민센터에서 확인하시기 바랍니다.
                </p>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </DashboardLayout>
  );
};

export default GuardianWelfare;
