import { useState, useEffect } from "react";
import {
  Search,
  Send,
  Clock,
  CheckCircle2,
  MessageSquare,
  Loader2
} from "lucide-react";
import DashboardLayout from "@/components/layout/DashboardLayout";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { ScrollArea } from "@/components/ui/scroll-area";
import { counselorNavItems } from "@/config/counselorNavItems";
import { toast } from "sonner";
import inquiriesApi from "@/api/inquiries";
import usersApi from "@/api/users";
import { InquiryResponse, MyProfileResponse } from "@/types/api";
import { useAuth } from "@/contexts/AuthContext";

interface InquiryWithMessages extends InquiryResponse {
  authorName?: string;
  messages?: Array<{
    id: number;
    sender: 'guardian' | 'counselor';
    content: string;
    timestamp: string;
  }>;
}

const CounselorInquiries = () => {
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedInquiry, setSelectedInquiry] = useState<InquiryWithMessages | null>(null);
  const [replyMessage, setReplyMessage] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [inquiries, setInquiries] = useState<InquiryWithMessages[]>([]);
  const { user } = useAuth();

  useEffect(() => {
    const fetchData = async () => {
      try {
        setIsLoading(true);

        // const profile = await usersApi.getMyProfile(); (Removed)
        // setUserProfile(profile); (Removed)

        const response = await inquiriesApi.getInquiries();
        // API returns InquiryResponse[] directly, not Page
        const data = Array.isArray(response) ? response : (response as any).content || [];

        const inquiriesWithMessages = data.map((inquiry: any) => ({
          ...inquiry,
          authorName: inquiry.elderlyName || '알 수 없음',
          messages: [
            {
              id: 1,
              sender: 'guardian' as const,
              content: inquiry.questionText,
              timestamp: inquiry.createdAt || ''
            },
            ...(inquiry.answerText ? [{
              id: 2,
              sender: 'counselor' as const,
              content: inquiry.answerText,
              timestamp: inquiry.answeredAt || ''
            }] : [])
          ]
        }));

        setInquiries(inquiriesWithMessages);
        if (inquiriesWithMessages.length > 0) {
          setSelectedInquiry(inquiriesWithMessages[0]);
        }
      } catch (error) {
        console.error('Failed to fetch inquiries:', error);
      } finally {
        setIsLoading(false);
      }
    };

    fetchData();
  }, []);

  const filteredInquiries = inquiries.filter((inquiry) =>
    inquiry.title?.includes(searchTerm) ||
    inquiry.authorName?.includes(searchTerm)
  );

  const waitingCount = inquiries.filter(i => i.status === 'PENDING').length;
  const answeredCount = inquiries.filter(i => i.status === 'ANSWERED').length;

  const handleReply = async () => {
    if (!replyMessage.trim() || !selectedInquiry) return;

    try {
      setIsSubmitting(true);
      await inquiriesApi.registerAnswer(selectedInquiry.id, { answerText: replyMessage });

      // Update local state
      const updatedInquiry: InquiryWithMessages = {
        ...selectedInquiry,
        status: 'ANSWERED',
        answerText: replyMessage,
        answeredAt: new Date().toISOString(),
        messages: [
          ...(selectedInquiry.messages || []),
          {
            id: (selectedInquiry.messages?.length || 0) + 1,
            sender: 'counselor',
            content: replyMessage,
            timestamp: new Date().toISOString()
          }
        ]
      };

      setInquiries(prev => prev.map(i => i.id === selectedInquiry.id ? updatedInquiry : i));
      setSelectedInquiry(updatedInquiry);
      setReplyMessage("");
      toast.success("답변이 등록되었습니다.");
    } catch (error) {
      console.error('Failed to submit reply:', error);
      toast.error("답변 등록에 실패했습니다.");
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) {
    return (
      <DashboardLayout role="counselor" userName="로딩중..." navItems={counselorNavItems}>
        <div className="flex items-center justify-center h-64">
          <Loader2 className="w-8 h-8 animate-spin text-primary" />
        </div>
      </DashboardLayout>
    );
  }

  const isAnswered = (status: string) => status === 'ANSWERED';

  return (
    <DashboardLayout
      role="counselor"
      userName={user?.name || "상담사"}
      navItems={counselorNavItems}
    >
      <div className="space-y-6">
        {/* Header */}
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-foreground">보호자 문의</h1>
            <p className="text-muted-foreground mt-1">보호자님들의 문의사항을 확인하고 답변하세요</p>
          </div>
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
            <Input
              placeholder="문의 검색..."
              className="pl-10 w-64"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>
        </div>

        {/* Stats */}
        <div className="grid grid-cols-3 gap-4">
          <Card className="shadow-card border-0">
            <CardContent className="p-4">
              <div className="flex items-center gap-3">
                <div className="p-2 rounded-full bg-primary/10">
                  <MessageSquare className="h-5 w-5 text-primary" />
                </div>
                <div>
                  <p className="text-2xl font-bold">{inquiries.length}</p>
                  <p className="text-sm text-muted-foreground">전체 문의</p>
                </div>
              </div>
            </CardContent>
          </Card>
          <Card className="shadow-card border-0">
            <CardContent className="p-4">
              <div className="flex items-center gap-3">
                <div className="p-2 rounded-full bg-warning/10">
                  <Clock className="h-5 w-5 text-warning" />
                </div>
                <div>
                  <p className="text-2xl font-bold">{waitingCount}</p>
                  <p className="text-sm text-muted-foreground">답변 대기</p>
                </div>
              </div>
            </CardContent>
          </Card>
          <Card className="shadow-card border-0">
            <CardContent className="p-4">
              <div className="flex items-center gap-3">
                <div className="p-2 rounded-full bg-success/10">
                  <CheckCircle2 className="h-5 w-5 text-success" />
                </div>
                <div>
                  <p className="text-2xl font-bold">{answeredCount}</p>
                  <p className="text-sm text-muted-foreground">답변 완료</p>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>

        {/* Main Content */}
        <div className="grid lg:grid-cols-3 gap-6">
          {/* Inquiry List */}
          <Card className="shadow-card border-0 lg:col-span-1">
            <CardHeader>
              <CardTitle className="text-lg">문의 목록</CardTitle>
            </CardHeader>
            <CardContent className="p-0">
              <ScrollArea className="h-[500px]">
                <div className="space-y-1 p-4 pt-0">
                  {filteredInquiries.length === 0 ? (
                    <div className="text-center py-8 text-muted-foreground">
                      {searchTerm ? '검색 결과가 없습니다.' : '문의가 없습니다.'}
                    </div>
                  ) : (
                    filteredInquiries.map((inquiry) => (
                      <div
                        key={inquiry.id}
                        className={`p-4 rounded-xl cursor-pointer transition-colors ${selectedInquiry?.id === inquiry.id
                          ? "bg-primary/10 border border-primary/30"
                          : "bg-secondary/30 hover:bg-secondary/50"
                          }`}
                        onClick={() => setSelectedInquiry(inquiry)}
                      >
                        <div className="flex items-start gap-3">
                          <Avatar className="w-10 h-10">
                            <AvatarFallback className="bg-primary/10 text-primary">
                              {inquiry.authorName?.charAt(0) || '?'}
                            </AvatarFallback>
                          </Avatar>
                          <div className="flex-1 min-w-0">
                            <div className="flex items-center justify-between gap-2">
                              <span className="font-medium text-sm truncate">{inquiry.authorName}</span>
                              <Badge
                                variant={isAnswered(inquiry.status) ? "secondary" : "destructive"}
                                className="text-xs"
                              >
                                {isAnswered(inquiry.status) ? "완료" : "대기"}
                              </Badge>
                            </div>
                            <p className="text-sm text-foreground mt-1 truncate">{inquiry.title}</p>
                            <p className="text-xs text-muted-foreground mt-1">
                              {inquiry.createdAt?.split('T')[0]}
                            </p>
                          </div>
                        </div>
                      </div>
                    ))
                  )}
                </div>
              </ScrollArea>
            </CardContent>
          </Card>

          {/* Message Detail */}
          <Card className="shadow-card border-0 lg:col-span-2">
            {selectedInquiry ? (
              <>
                <CardHeader className="border-b">
                  <div className="flex items-start justify-between">
                    <div>
                      <CardTitle>{selectedInquiry.title}</CardTitle>
                      <CardDescription className="mt-1">
                        {selectedInquiry.authorName}
                      </CardDescription>
                    </div>
                    <Badge
                      variant={isAnswered(selectedInquiry.status) ? "secondary" : "destructive"}
                    >
                      {isAnswered(selectedInquiry.status) ? "답변 완료" : "답변 대기"}
                    </Badge>
                  </div>
                </CardHeader>
                <CardContent className="p-0">
                  <ScrollArea className="h-[350px] p-6">
                    <div className="space-y-4">
                      {selectedInquiry.messages?.map((message) => (
                        <div
                          key={message.id}
                          className={`flex ${message.sender === "counselor" ? "justify-end" : "justify-start"}`}
                        >
                          <div
                            className={`max-w-[80%] p-4 rounded-2xl ${message.sender === "counselor"
                              ? "bg-primary text-primary-foreground rounded-br-sm"
                              : "bg-secondary rounded-bl-sm"
                              }`}
                          >
                            <p className="text-sm">{message.content}</p>
                            <p className={`text-xs mt-2 ${message.sender === "counselor" ? "text-primary-foreground/70" : "text-muted-foreground"
                              }`}>
                              {message.timestamp?.split('T')[0]}
                            </p>
                          </div>
                        </div>
                      ))}
                    </div>
                  </ScrollArea>
                  {!isAnswered(selectedInquiry.status) && (
                    <div className="p-4 border-t">
                      <div className="flex gap-2">
                        <Textarea
                          placeholder="답변을 입력하세요..."
                          value={replyMessage}
                          onChange={(e) => setReplyMessage(e.target.value)}
                          className="min-h-[80px]"
                        />
                        <Button
                          className="px-4"
                          onClick={handleReply}
                          disabled={!replyMessage.trim() || isSubmitting}
                        >
                          {isSubmitting ? (
                            <Loader2 className="w-5 h-5 animate-spin" />
                          ) : (
                            <Send className="w-5 h-5" />
                          )}
                        </Button>
                      </div>
                    </div>
                  )}
                </CardContent>
              </>
            ) : (
              <div className="flex items-center justify-center h-[500px] text-muted-foreground">
                문의를 선택해주세요
              </div>
            )}
          </Card>
        </div>
      </div>
    </DashboardLayout>
  );
};

export default CounselorInquiries;
