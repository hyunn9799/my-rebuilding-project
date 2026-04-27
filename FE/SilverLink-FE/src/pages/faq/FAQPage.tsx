import { useState, useEffect } from "react";
import {
  HelpCircle,
  Search,
  ChevronRight,
  ChevronLeft,
  Book,
  Loader2
} from "lucide-react";
import { useAuth } from "@/contexts/AuthContext";
import { guardianNavItems } from "@/config/guardianNavItems";
import DashboardLayout from "@/components/layout/DashboardLayout";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from "@/components/ui/accordion";
import faqsApi from "@/api/faqs";
import { FaqResponse } from "@/types/api";

// 백엔드 카테고리와 일치하도록 수정 (SERVICE, CALLBOT, MEDICATION, WELFARE)
const faqCategories = [
  { id: "SERVICE", name: "서비스 이용" },
  { id: "CALLBOT", name: "콜봇 관련" },
  { id: "MEDICATION", name: "복약 관리" },
  { id: "WELFARE", name: "복지 서비스" },
];

const getCategoryName = (categoryId: string): string => {
  const category = faqCategories.find(c => c.id === categoryId);
  return category?.name || categoryId;
};

const PAGE_SIZE = 10;

const FAQPage = () => {
  const { user } = useAuth();
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedCategory, setSelectedCategory] = useState("all");
  const [isLoading, setIsLoading] = useState(true);
  const [faqs, setFaqs] = useState<FaqResponse[]>([]);
  const [currentPage, setCurrentPage] = useState(0);

  useEffect(() => {
    const fetchFaqs = async () => {
      try {
        setIsLoading(true);
        const data = await faqsApi.getFaqs();
        setFaqs(data);
      } catch (error) {
        console.error('Failed to fetch FAQs:', error);
      } finally {
        setIsLoading(false);
      }
    };

    fetchFaqs();
  }, []);

  // 필터된 FAQ 리스트
  const filteredFAQ = faqs.filter(item => {
    const matchesSearch = item.question?.includes(searchQuery) || item.answerText?.includes(searchQuery);
    const matchesCategory = selectedCategory === "all" || item.category === selectedCategory;
    return matchesSearch && matchesCategory;
  });

  // 페이지네이션 계산
  const totalPages = Math.ceil(filteredFAQ.length / PAGE_SIZE);
  const paginatedFAQ = filteredFAQ.slice(currentPage * PAGE_SIZE, (currentPage + 1) * PAGE_SIZE);

  // 카테고리별 개수 계산
  const getCategoryCount = (categoryId: string): number => {
    return faqs.filter(faq => faq.category === categoryId).length;
  };

  // 카테고리나 검색어 변경 시 페이지 초기화
  useEffect(() => {
    setCurrentPage(0);
  }, [searchQuery, selectedCategory]);

  const handlePageChange = (page: number) => {
    if (page >= 0 && page < totalPages) {
      setCurrentPage(page);
    }
  };

  if (isLoading) {
    return (
      <DashboardLayout role="guardian" userName="로딩중..." navItems={guardianNavItems}>
        <div className="flex items-center justify-center min-h-[400px]">
          <Loader2 className="w-12 h-12 animate-spin text-primary" />
        </div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout
      role="guardian"
      userName={user?.name || "보호자"}
      navItems={guardianNavItems}
    >
      <div className="space-y-6">
        {/* Page Header */}
        <div>
          <h1 className="text-2xl font-bold text-foreground">자주 묻는 질문</h1>
          <p className="text-muted-foreground mt-1">서비스 이용에 대한 궁금한 점을 확인하세요</p>
        </div>

        {/* Search */}
        <Card className="shadow-card border-0">
          <CardContent className="p-6">
            <div className="relative">
              <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-muted-foreground" />
              <Input
                placeholder="궁금한 내용을 검색하세요..."
                className="pl-12 h-12 text-lg"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
              />
            </div>
          </CardContent>
        </Card>

        {/* Categories */}
        <div className="flex flex-wrap gap-2">
          <Badge
            variant={selectedCategory === "all" ? "default" : "outline"}
            className="cursor-pointer px-4 py-2"
            onClick={() => setSelectedCategory("all")}
          >
            전체 ({faqs.length})
          </Badge>
          {faqCategories.map((category) => (
            <Badge
              key={category.id}
              variant={selectedCategory === category.id ? "default" : "outline"}
              className="cursor-pointer px-4 py-2"
              onClick={() => setSelectedCategory(category.id)}
            >
              {category.name} ({getCategoryCount(category.id)})
            </Badge>
          ))}
        </div>

        {/* FAQ List */}
        <Card className="shadow-card border-0">
          <CardContent className="p-6">
            {/* 현재 페이지 정보 */}
            {filteredFAQ.length > 0 && (
              <div className="text-sm text-muted-foreground mb-4">
                총 {filteredFAQ.length}개 중 {currentPage * PAGE_SIZE + 1}-{Math.min((currentPage + 1) * PAGE_SIZE, filteredFAQ.length)}개 표시
              </div>
            )}

            <Accordion type="single" collapsible className="space-y-2">
              {paginatedFAQ.map((item) => (
                <AccordionItem key={item.id} value={item.id.toString()} className="border rounded-xl px-4">
                  <AccordionTrigger className="hover:no-underline py-4">
                    <div className="flex items-center gap-3 text-left">
                      <div className="w-8 h-8 rounded-full bg-primary/10 flex items-center justify-center flex-shrink-0">
                        <HelpCircle className="w-4 h-4 text-primary" />
                      </div>
                      <span className="font-medium">{item.question}</span>
                    </div>
                  </AccordionTrigger>
                  <AccordionContent className="pb-4 pl-11">
                    <p className="text-muted-foreground leading-relaxed">{item.answerText}</p>
                    <div className="flex items-center gap-4 mt-4 pt-4 border-t text-sm text-muted-foreground">
                      <Badge variant="outline">
                        {getCategoryName(item.category)}
                      </Badge>
                    </div>
                  </AccordionContent>
                </AccordionItem>
              ))}
            </Accordion>

            {filteredFAQ.length === 0 && (
              <div className="text-center py-12">
                <Book className="w-12 h-12 text-muted-foreground mx-auto mb-4" />
                <p className="text-muted-foreground">검색 결과가 없습니다</p>
              </div>
            )}

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
          </CardContent>
        </Card>

        {/* Contact Section */}
        <Card className="shadow-card border-0 bg-gradient-primary text-primary-foreground">
          <CardContent className="p-6">
            <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
              <div>
                <h3 className="text-lg font-semibold">원하는 답변을 찾지 못하셨나요?</h3>
                <p className="text-primary-foreground/80 mt-1">담당 상담사에게 직접 문의하세요</p>
              </div>
              <a href="/guardian/inquiry" className="inline-flex items-center gap-2 px-6 py-3 bg-primary-foreground/20 hover:bg-primary-foreground/30 rounded-xl transition-colors">
                1:1 문의하기 <ChevronRight className="w-4 h-4" />
              </a>
            </div>
          </CardContent>
        </Card>
      </div>
    </DashboardLayout>
  );
};

export default FAQPage;
