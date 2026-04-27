import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { Heart, ArrowLeft, Phone, Loader2, CheckCircle2, User } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { useToast } from "@/hooks/use-toast";
import phoneVerificationApi from "@/api/phoneVerification";
import { findId } from "@/api/auth";

const ForgotId = () => {
    const navigate = useNavigate();
    const { toast } = useToast();

    const [step, setStep] = useState<'phone' | 'verify' | 'result'>("phone");
    const [isLoading, setIsLoading] = useState(false);
    const [foundId, setFoundId] = useState("");

    const [verificationId, setVerificationId] = useState<number | null>(null);
    const [formData, setFormData] = useState({
        name: "",
        phone: "",
        verificationCode: "",
    });

    const handleSendCode = async () => {
        if (!formData.phone || !formData.name) {
            toast({
                title: "입력 오류",
                description: "이름과 휴대폰 번호를 모두 입력해주세요.",
                variant: "destructive",
            });
            return;
        }

        try {
            setIsLoading(true);
            const result = await phoneVerificationApi.requestVerificationCode(formData.phone, 'SIGNUP');
            setVerificationId(result.verificationId);
            toast({
                title: "인증번호 발송",
                description: "입력하신 휴대폰으로 인증번호가 발송되었습니다.",
            });
            setStep("verify");
        } catch (error) {
            console.error("Failed to send code:", error);
            toast({
                title: "발송 실패",
                description: "인증번호 발송에 실패했습니다. 잠시 후 다시 시도해주세요.",
                variant: "destructive",
            });
        } finally {
            setIsLoading(false);
        }
    };

    const handleVerifyCode = async () => {
        if (!formData.verificationCode) {
            toast({
                title: "입력 오류",
                description: "인증번호를 입력해주세요.",
                variant: "destructive",
            });
            return;
        }

        try {
            setIsLoading(true);
            if (!verificationId) throw new Error('Verification ID not found');
            const verifyResult = await phoneVerificationApi.verifyCode(verificationId, formData.verificationCode);

            // 실제 아이디 찾기 API 호출
            const result = await findId(formData.name, verifyResult.proofToken);
            setFoundId(result.maskedLoginId);
            setStep("result");
        } catch (error) {
            console.error("Failed to verify code:", error);
            toast({
                title: "인증 실패",
                description: "인증번호가 일치하지 않거나 사용자를 찾을 수 없습니다.",
                variant: "destructive",
            });
        } finally {
            setIsLoading(false);
        }
    };


    return (
        <div className="min-h-screen bg-gradient-hero flex items-center justify-center p-4">
            <div className="w-full max-w-md">
                {/* 로고 */}
                <div className="text-center mb-8">
                    <Link to="/" className="inline-flex items-center gap-2">
                        <div className="w-10 h-10 rounded-xl bg-gradient-primary flex items-center justify-center">
                            <Heart className="w-6 h-6 text-primary-foreground" />
                        </div>
                        <span className="text-2xl font-bold text-foreground">마음돌봄</span>
                    </Link>
                </div>

                <Card className="shadow-card border-0">
                    <CardHeader className="text-center">
                        <CardTitle className="text-xl">아이디 찾기</CardTitle>
                        <CardDescription>
                            {step === 'phone' && "가입 시 등록한 이름과 휴대폰 번호로 인증해주세요"}
                            {step === 'verify' && "휴대폰으로 전송된 인증번호를 입력해주세요"}
                            {step === 'result' && "아이디 찾기가 완료되었습니다"}
                        </CardDescription>
                    </CardHeader>

                    <CardContent className="space-y-4">
                        {step === 'phone' && (
                            <>
                                <div className="space-y-2">
                                    <Label htmlFor="name">
                                        <User className="w-4 h-4 inline mr-2" />
                                        이름
                                    </Label>
                                    <Input
                                        id="name"
                                        placeholder="가입 시 등록한 이름"
                                        value={formData.name}
                                        onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                                    />
                                </div>
                                <div className="space-y-2">
                                    <Label htmlFor="phone">휴대폰 번호</Label>
                                    <div className="flex gap-2">
                                        <Input
                                            id="phone"
                                            placeholder="010-0000-0000"
                                            value={formData.phone}
                                            onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                                        />
                                        <Button onClick={handleSendCode} disabled={isLoading}>
                                            {isLoading ? (
                                                <Loader2 className="w-4 h-4 animate-spin" />
                                            ) : (
                                                "인증요청"
                                            )}
                                        </Button>
                                    </div>
                                </div>
                            </>
                        )}

                        {step === 'verify' && (
                            <div className="space-y-4">
                                <div className="p-3 bg-primary/10 rounded-lg text-sm text-center">
                                    <Phone className="w-4 h-4 inline mr-2" />
                                    {formData.phone}로 인증번호가 발송되었습니다
                                </div>
                                <div className="space-y-2">
                                    <Label htmlFor="code">인증번호</Label>
                                    <Input
                                        id="code"
                                        placeholder="6자리 인증번호"
                                        value={formData.verificationCode}
                                        onChange={(e) => setFormData({ ...formData, verificationCode: e.target.value })}
                                        maxLength={6}
                                    />
                                </div>
                                <Button className="w-full" onClick={handleVerifyCode} disabled={isLoading}>
                                    {isLoading ? (
                                        <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                                    ) : (
                                        "인증 확인"
                                    )}
                                </Button>
                                <Button variant="ghost" className="w-full" onClick={handleSendCode} disabled={isLoading}>
                                    인증번호 재발송
                                </Button>
                            </div>
                        )}

                        {step === 'result' && (
                            <div className="text-center space-y-4">
                                <div className="w-16 h-16 mx-auto rounded-full bg-success/10 flex items-center justify-center">
                                    <CheckCircle2 className="w-8 h-8 text-success" />
                                </div>
                                <div className="p-4 bg-muted rounded-lg">
                                    <p className="text-sm text-muted-foreground mb-2">회원님의 아이디는</p>
                                    <p className="text-xl font-bold text-foreground">{foundId}</p>
                                    <p className="text-xs text-muted-foreground mt-2">
                                        보안을 위해 일부 문자가 가려져 있습니다
                                    </p>
                                </div>
                                <div className="flex gap-2">
                                    <Button variant="outline" className="flex-1" onClick={() => navigate("/forgot-password")}>
                                        비밀번호 찾기
                                    </Button>
                                    <Button className="flex-1" onClick={() => navigate("/login")}>
                                        로그인
                                    </Button>
                                </div>
                            </div>
                        )}

                        {step !== 'result' && (
                            <div className="pt-4 border-t">
                                <Link
                                    to="/login"
                                    className="flex items-center justify-center gap-2 text-sm text-muted-foreground hover:text-primary"
                                >
                                    <ArrowLeft className="w-4 h-4" />
                                    로그인으로 돌아가기
                                </Link>
                            </div>
                        )}
                    </CardContent>
                </Card>

                <div className="text-center mt-4">
                    <Link to="/forgot-password" className="text-sm text-muted-foreground hover:text-primary">
                        비밀번호가 기억나지 않으세요? <span className="text-primary">비밀번호 찾기</span>
                    </Link>
                </div>
            </div>
        </div>
    );
};

export default ForgotId;
