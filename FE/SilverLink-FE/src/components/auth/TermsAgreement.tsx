import { useState } from "react";
import { Check, ChevronDown, ChevronUp, FileText } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Checkbox } from "@/components/ui/checkbox";
import { Label } from "@/components/ui/label";
import { ScrollArea } from "@/components/ui/scroll-area";
import {
    Collapsible,
    CollapsibleContent,
    CollapsibleTrigger,
} from "@/components/ui/collapsible";

interface Term {
    id: string;
    title: string;
    required: boolean;
    content: string;
}

interface TermsAgreementProps {
    onAgree: (agreedTerms: string[]) => void;
    onCancel?: () => void;
    terms?: Term[];
}

const defaultTerms: Term[] = [
    {
        id: "service",
        title: "서비스 이용약관",
        required: true,
        content: `제1조 (목적)
이 약관은 마음돌봄 서비스(이하 "서비스")의 이용조건 및 절차, 회원과 회사의 권리, 의무, 책임사항과 기타 필요한 사항을 규정함을 목적으로 합니다.

제2조 (용어의 정의)
1. "회원"이란 서비스에 접속하여 이 약관에 따라 회사와 이용계약을 체결하고 서비스를 이용하는 자를 말합니다.
2. "보호자"란 어르신의 보호 및 돌봄을 위해 서비스를 이용하는 가족 또는 법정 대리인을 말합니다.
3. "어르신"이란 AI 안부 전화 서비스의 대상이 되는 고령자를 말합니다.

제3조 (서비스의 제공)
1. 회사는 다음과 같은 서비스를 제공합니다:
   - AI 기반 안부 전화 서비스
   - 어르신 상태 모니터링 및 리포트
   - 보호자 알림 서비스
   - 상담사 연결 서비스

제4조 (개인정보 보호)
회사는 관련 법령이 정하는 바에 따라 회원의 개인정보를 보호하기 위해 노력합니다.`,
    },
    {
        id: "privacy",
        title: "개인정보 수집 및 이용 동의",
        required: true,
        content: `1. 수집하는 개인정보 항목
   - 필수: 이름, 휴대폰 번호, 이메일 주소
   - 선택: 주소, 생년월일

2. 개인정보의 수집 및 이용 목적
   - 서비스 제공 및 운영
   - 회원 관리 및 본인 확인
   - 서비스 개선 및 통계 분석
   - 긴급 상황 시 보호자 연락

3. 개인정보의 보유 및 이용 기간
   - 회원 탈퇴 시까지 또는 법령에 따른 보존 기간

4. 동의 거부 권리 및 불이익
   - 개인정보 수집에 동의하지 않을 권리가 있습니다.
   - 다만, 필수 항목 미동의 시 서비스 이용이 제한됩니다.`,
    },
    {
        id: "sensitive",
        title: "민감정보 수집 및 이용 동의 (선택)",
        required: false,
        content: `1. 수집하는 민감정보 항목
   - 건강 관련 정보 (복용 중인 약, 기저질환)
   - 통화 내용 분석을 통한 감정 상태

2. 민감정보의 수집 및 이용 목적
   - 어르신의 건강 상태 모니터링
   - 이상 징후 감지 및 보호자 알림
   - 맞춤형 상담 서비스 제공

3. 민감정보의 보유 및 이용 기간
   - 동의 철회 시 또는 회원 탈퇴 시까지

4. 동의 거부 권리 및 불이익
   - 선택 항목으로, 동의하지 않아도 기본 서비스 이용이 가능합니다.
   - 다만, 일부 고급 기능(건강 모니터링 등) 이용이 제한될 수 있습니다.`,
    },
    {
        id: "marketing",
        title: "마케팅 정보 수신 동의 (선택)",
        required: false,
        content: `1. 마케팅 정보 수신 항목
   - 서비스 업데이트 및 새로운 기능 안내
   - 이벤트 및 프로모션 정보
   - 복지 서비스 관련 정보

2. 수신 방법
   - SMS, 이메일, 앱 푸시 알림

3. 동의 거부 권리
   - 마케팅 정보 수신에 동의하지 않을 권리가 있습니다.
   - 동의하지 않아도 서비스 이용에 제한이 없습니다.
   - 동의 후에도 언제든지 설정에서 수신 거부할 수 있습니다.`,
    },
];

const TermsAgreement = ({
    onAgree,
    onCancel,
    terms = defaultTerms,
}: TermsAgreementProps) => {
    const [agreedTerms, setAgreedTerms] = useState<Set<string>>(new Set());
    const [expandedTerms, setExpandedTerms] = useState<Set<string>>(new Set());

    const requiredTerms = terms.filter((t) => t.required);
    const allRequiredAgreed = requiredTerms.every((t) => agreedTerms.has(t.id));

    const handleToggle = (termId: string) => {
        const newAgreed = new Set(agreedTerms);
        if (newAgreed.has(termId)) {
            newAgreed.delete(termId);
        } else {
            newAgreed.add(termId);
        }
        setAgreedTerms(newAgreed);
    };

    const handleToggleAll = () => {
        if (agreedTerms.size === terms.length) {
            setAgreedTerms(new Set());
        } else {
            setAgreedTerms(new Set(terms.map((t) => t.id)));
        }
    };

    const handleExpand = (termId: string) => {
        const newExpanded = new Set(expandedTerms);
        if (newExpanded.has(termId)) {
            newExpanded.delete(termId);
        } else {
            newExpanded.add(termId);
        }
        setExpandedTerms(newExpanded);
    };

    const handleAgree = () => {
        onAgree(Array.from(agreedTerms));
    };

    return (
        <Card className="shadow-card border-0 max-w-lg w-full">
            <CardHeader className="text-center pb-2">
                <div className="w-12 h-12 mx-auto rounded-full bg-primary/10 flex items-center justify-center mb-2">
                    <FileText className="w-6 h-6 text-primary" />
                </div>
                <CardTitle>약관 동의</CardTitle>
                <CardDescription>
                    서비스 이용을 위해 아래 약관에 동의해주세요
                </CardDescription>
            </CardHeader>

            <CardContent className="space-y-4">
                {/* 전체 동의 */}
                <div
                    className="flex items-center gap-3 p-4 bg-muted/50 rounded-lg cursor-pointer hover:bg-muted/70 transition-colors"
                    onClick={handleToggleAll}
                >
                    <Checkbox
                        id="all"
                        checked={agreedTerms.size === terms.length}
                        onCheckedChange={handleToggleAll}
                    />
                    <Label htmlFor="all" className="cursor-pointer font-semibold flex-1">
                        전체 동의
                    </Label>
                </div>

                {/* 개별 약관 */}
                <div className="space-y-2">
                    {terms.map((term) => (
                        <Collapsible
                            key={term.id}
                            open={expandedTerms.has(term.id)}
                            onOpenChange={() => handleExpand(term.id)}
                        >
                            <div className="border rounded-lg">
                                <div className="flex items-center gap-3 p-3">
                                    <Checkbox
                                        id={term.id}
                                        checked={agreedTerms.has(term.id)}
                                        onCheckedChange={() => handleToggle(term.id)}
                                        onClick={(e) => e.stopPropagation()}
                                    />
                                    <Label
                                        htmlFor={term.id}
                                        className="cursor-pointer flex-1 flex items-center gap-2"
                                    >
                                        <span
                                            className={`text-xs px-1.5 py-0.5 rounded ${term.required
                                                    ? "bg-destructive/10 text-destructive"
                                                    : "bg-muted text-muted-foreground"
                                                }`}
                                        >
                                            {term.required ? "필수" : "선택"}
                                        </span>
                                        <span className="text-sm">{term.title}</span>
                                    </Label>
                                    <CollapsibleTrigger asChild>
                                        <Button variant="ghost" size="icon" className="h-8 w-8">
                                            {expandedTerms.has(term.id) ? (
                                                <ChevronUp className="w-4 h-4" />
                                            ) : (
                                                <ChevronDown className="w-4 h-4" />
                                            )}
                                        </Button>
                                    </CollapsibleTrigger>
                                </div>

                                <CollapsibleContent>
                                    <ScrollArea className="h-40 px-3 pb-3">
                                        <p className="text-sm text-muted-foreground whitespace-pre-line">
                                            {term.content}
                                        </p>
                                    </ScrollArea>
                                </CollapsibleContent>
                            </div>
                        </Collapsible>
                    ))}
                </div>

                {/* 버튼 영역 */}
                <div className="flex gap-2 pt-2">
                    {onCancel && (
                        <Button variant="outline" className="flex-1" onClick={onCancel}>
                            취소
                        </Button>
                    )}
                    <Button
                        className="flex-1"
                        onClick={handleAgree}
                        disabled={!allRequiredAgreed}
                    >
                        <Check className="w-4 h-4 mr-2" />
                        동의하고 계속하기
                    </Button>
                </div>

                {!allRequiredAgreed && (
                    <p className="text-xs text-center text-muted-foreground">
                        필수 약관에 모두 동의해야 다음 단계로 진행할 수 있습니다
                    </p>
                )}
            </CardContent>
        </Card>
    );
};

export default TermsAgreement;
