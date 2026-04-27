import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from "@/components/ui/accordion";
import {
  ArrowLeft,
  HelpCircle,
  Search,
  Phone,
  Heart,
  Calendar,
  Shield,
  Volume2,
  Loader2,
  ChevronLeft,
  ChevronRight
} from "lucide-react";
import faqsApi from "@/api/faqs";
import { FaqResponse } from "@/types/api";

// 백엔드 카테고리와 일치하도록 수정 (SERVICE, CALLBOT, MEDICATION, WELFARE)
const categories = [
  { id: "all", name: "전체", icon: <HelpCircle className="w-6 h-6" /> },
  { id: "SERVICE", name: "서비스", icon: <Phone className="w-6 h-6" /> },
  { id: "CALLBOT", name: "콜봇", icon: <Heart className="w-6 h-6" /> },
  { id: "MEDICATION", name: "복약", icon: <Calendar className="w-6 h-6" /> },
  { id: "WELFARE", name: "복지", icon: <Shield className="w-6 h-6" /> },
];

const PAGE_SIZE = 10;

const SeniorFAQ = () => {
  const navigate = useNavigate();
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedCategory, setSelectedCategory] = useState("all");
  const [speakingId, setSpeakingId] = useState<number | null>(null);
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

  const filteredFaqs = faqs.filter((faq) => {
    const matchesSearch =
      faq.question?.includes(searchTerm) ||
      faq.answerText?.includes(searchTerm);
    const matchesCategory =
      selectedCategory === "all" ||
      faq.category === selectedCategory;
    return matchesSearch && matchesCategory;
  });

  // 페이지네이션 계산
  const totalPages = Math.ceil(filteredFaqs.length / PAGE_SIZE);
  const paginatedFaqs = filteredFaqs.slice(currentPage * PAGE_SIZE, (currentPage + 1) * PAGE_SIZE);

  // 카테고리나 검색어 변경 시 페이지 초기화
  useEffect(() => {
    setCurrentPage(0);
  }, [searchTerm, selectedCategory]);

  const handlePageChange = (page: number) => {
    if (page >= 0 && page < totalPages) {
      setCurrentPage(page);
    }
  };

  const handleSpeak = (faq: FaqResponse) => {
    if (speakingId === faq.id) {
      window.speechSynthesis.cancel();
      setSpeakingId(null);
      return;
    }

    window.speechSynthesis.cancel();
    const text = `질문: ${faq.question}. 답변: ${faq.answerText}`;
    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = "ko-KR";
    utterance.rate = 0.8;
    utterance.onend = () => setSpeakingId(null);

    window.speechSynthesis.speak(utterance);
    setSpeakingId(faq.id);
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-background flex items-center justify-center">
        <Loader2 className="w-12 h-12 animate-spin text-primary" />
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background pb-6">
      {/* Header */}
      <header className="bg-accent text-accent-foreground p-6 rounded-b-3xl shadow-lg">
        <div className="flex items-center gap-4">
          <Button
            variant="ghost"
            size="lg"
            onClick={() => navigate("/senior")}
            className="text-accent-foreground hover:bg-accent-foreground/20 p-3"
          >
            <ArrowLeft className="w-8 h-8" />
          </Button>
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 rounded-xl bg-accent-foreground/20 flex items-center justify-center">
              <HelpCircle className="w-7 h-7" />
            </div>
            <div>
              <h1 className="text-2xl font-bold">자주 묻는 질문</h1>
              <p className="text-accent-foreground/80 text-sm">궁금한 것을 찾아보세요</p>
            </div>
          </div>
        </div>
      </header>

      <main className="p-6 space-y-6">
        {/* Search */}
        <div className="relative">
          <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-6 h-6 text-muted-foreground" />
          <Input
            placeholder="궁금한 것을 검색하세요"
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="pl-14 h-16 text-lg rounded-2xl"
          />
        </div>

        {/* Categories */}
        <div className="flex gap-3 overflow-x-auto pb-2">
          {categories.map((category) => (
            <Button
              key={category.id}
              variant={selectedCategory === category.id ? "default" : "outline"}
              onClick={() => setSelectedCategory(category.id)}
              className="h-14 px-5 rounded-xl flex-shrink-0 gap-2"
            >
              {category.icon}
              <span className="font-bold">{category.name}</span>
            </Button>
          ))}
        </div>

        {/* FAQ List */}
        <Card>
          <CardContent className="p-4">
            {/* 현재 페이지 정보 */}
            {filteredFaqs.length > 0 && (
              <div className="text-sm text-muted-foreground mb-4 text-center">
                총 {filteredFaqs.length}개 중 {currentPage * PAGE_SIZE + 1}-{Math.min((currentPage + 1) * PAGE_SIZE, filteredFaqs.length)}개
              </div>
            )}

            {paginatedFaqs.length === 0 ? (
              <div className="text-center py-12">
                <HelpCircle className="w-16 h-16 mx-auto text-muted-foreground/50 mb-4" />
                <p className="text-lg text-muted-foreground">
                  {searchTerm || selectedCategory !== "all" ? "검색 결과가 없어요" : "등록된 FAQ가 없어요"}
                </p>
              </div>
            ) : (
              <Accordion type="single" collapsible className="space-y-3">
                {paginatedFaqs.map((faq) => (
                  <AccordionItem
                    key={faq.id}
                    value={String(faq.id)}
                    className="border rounded-xl px-4"
                  >
                    <AccordionTrigger className="text-left text-lg font-bold py-5 hover:no-underline">
                      <div className="flex items-start gap-3">
                        <HelpCircle className="w-6 h-6 text-accent flex-shrink-0 mt-0.5" />
                        <span>{faq.question}</span>
                      </div>
                    </AccordionTrigger>
                    <AccordionContent className="pb-5">
                      <div className="pl-9 space-y-4">
                        <p className="text-lg leading-relaxed text-muted-foreground">
                          {faq.answerText}
                        </p>
                        <Button
                          variant="outline"
                          onClick={() => handleSpeak(faq)}
                          className={`h-12 rounded-xl gap-2 ${speakingId === faq.id ? "bg-warning/10 border-warning text-warning" : ""
                            }`}
                        >
                          <Volume2 className="w-5 h-5" />
                          {speakingId === faq.id ? "읽기 중지" : "소리로 듣기"}
                        </Button>
                      </div>
                    </AccordionContent>
                  </AccordionItem>
                ))}
              </Accordion>
            )}

            {/* 페이지네이션 UI - 어르신을 위한 큰 버튼 */}
            {totalPages > 1 && (
              <div className="flex items-center justify-center gap-3 mt-6">
                <Button
                  variant="outline"
                  size="lg"
                  onClick={() => handlePageChange(currentPage - 1)}
                  disabled={currentPage === 0}
                  className="h-14 px-6 text-lg rounded-xl"
                >
                  <ChevronLeft className="w-6 h-6 mr-1" />
                  이전
                </Button>

                <div className="flex items-center gap-2">
                  {Array.from({ length: Math.min(3, totalPages) }, (_, i) => {
                    let pageNum;
                    if (totalPages <= 3) {
                      pageNum = i;
                    } else if (currentPage < 2) {
                      pageNum = i;
                    } else if (currentPage > totalPages - 3) {
                      pageNum = totalPages - 3 + i;
                    } else {
                      pageNum = currentPage - 1 + i;
                    }
                    return (
                      <Button
                        key={pageNum}
                        variant={currentPage === pageNum ? "default" : "outline"}
                        size="lg"
                        onClick={() => handlePageChange(pageNum)}
                        className="w-14 h-14 text-lg rounded-xl"
                      >
                        {pageNum + 1}
                      </Button>
                    );
                  })}
                </div>

                <Button
                  variant="outline"
                  size="lg"
                  onClick={() => handlePageChange(currentPage + 1)}
                  disabled={currentPage >= totalPages - 1}
                  className="h-14 px-6 text-lg rounded-xl"
                >
                  다음
                  <ChevronRight className="w-6 h-6 ml-1" />
                </Button>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Contact Info */}
        <Card className="bg-primary/5 border-primary/20">
          <CardContent className="p-6 text-center space-y-4">
            <p className="text-lg font-bold">찾는 답이 없으신가요?</p>
            <p className="text-muted-foreground">
              담당 상담사에게 직접 물어보세요
            </p>
            <Button
              onClick={() => navigate("/senior")}
              className="h-14 px-8 text-lg font-bold rounded-xl gap-2"
            >
              <Phone className="w-5 h-5" />
              홈으로 가서 전화하기
            </Button>
          </CardContent>
        </Card>
      </main>
    </div>
  );
};

export default SeniorFAQ;
