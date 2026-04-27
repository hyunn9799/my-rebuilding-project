import { useState } from "react";
import { useNavigate, Link } from "react-router-dom";
import { Heart, ArrowLeft, Phone, KeyRound, Loader2, CheckCircle2 } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { useToast } from "@/hooks/use-toast";
import phoneVerificationApi from "@/api/phoneVerification";
import { resetPassword } from "@/api/auth";

const ForgotPassword = () => {
    const navigate = useNavigate();
    const { toast } = useToast();

    const [step, setStep] = useState<'phone' | 'verify' | 'reset' | 'complete'>("phone");
    const [isLoading, setIsLoading] = useState(false);

    const [verificationId, setVerificationId] = useState<number | null>(null);
    const [proofToken, setProofToken] = useState<string>("");
    const [formData, setFormData] = useState({
        loginId: "",
        phone: "",
        verificationCode: "",
        newPassword: "",
        confirmPassword: "",
    });

    const handleSendCode = async () => {
        if (!formData.phone || !formData.loginId) {
            toast({
                title: "입력 오류",
                description: "아이디와 휴대폰 번호를 모두 입력해주세요.",
                variant: "destructive",
            });
            return;
        }

        try {
            setIsLoading(true);
            const result = await phoneVerificationApi.requestVerificationCode(formData.phone, 'PASSWORD_RESET');
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
            const result = await phoneVerificationApi.verifyCode(verificationId, formData.verificationCode);
            setProofToken(result.proofToken);
            toast({
                title: "인증 성공",
                description: "휴대폰 인증이 완료되었습니다.",
            });
            setStep("reset");
        } catch (error) {
            console.error("Failed to verify code:", error);
            toast({
                title: "인증 실패",
                description: "인증번호가 일치하지 않습니다.",
                variant: "destructive",
            });
        } finally {
            setIsLoading(false);
        }
    };

    const handleResetPassword = async () => {
        if (!formData.newPassword || !formData.confirmPassword) {
            toast({
                title: "입력 오류",
                description: "새 비밀번호를 입력해주세요.",
                variant: "destructive",
            });
            return;
        }

        if (formData.newPassword !== formData.confirmPassword) {
            toast({
                title: "입력 오류",
                description: "비밀번호가 일치하지 않습니다.",
                variant: "destructive",
            });
            return;
        }

        if (formData.newPassword.length < 8) {
            toast({
                title: "입력 오류",
                description: "비밀번호는 8자 이상이어야 합니다.",
                variant: "destructive",
            });
            return;
        }

        try {
            setIsLoading(true);
            await resetPassword(formData.loginId, proofToken, formData.newPassword);
            setStep("complete");
        } catch (error) {
            console.error("Failed to reset password:", error);
            toast({
                title: "비밀번호 변경 실패",
                description: "비밀번호 변경에 실패했습니다. 잠시 후 다시 시도해주세요.",
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
                        <CardTitle className="text-xl">비밀번호 찾기</CardTitle>
                        <CardDescription>
                            {step === 'phone' && "가입 시 등록한 휴대폰 번호로 인증해주세요"}
                            {step === 'verify' && "휴대폰으로 전송된 인증번호를 입력해주세요"}
                            {step === 'reset' && "새로운 비밀번호를 설정해주세요"}
                            {step === 'complete' && "비밀번호가 성공적으로 변경되었습니다"}
                        </CardDescription>
                    </CardHeader>

                    <CardContent className="space-y-4">
                        {step === 'phone' && (
                            <>
                                <div className="space-y-2">
                                    <Label htmlFor="loginId">아이디</Label>
                                    <Input
                                        id="loginId"
                                        placeholder="가입 시 사용한 아이디"
                                        value={formData.loginId}
                                        onChange={(e) => setFormData({ ...formData, loginId: e.target.value })}
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

                        {step === 'reset' && (
                            <div className="space-y-4">
                                <div className="space-y-2">
                                    <Label htmlFor="newPassword">
                                        <KeyRound className="w-4 h-4 inline mr-2" />
                                        새 비밀번호
                                    </Label>
                                    <Input
                                        id="newPassword"
                                        type="password"
                                        placeholder="8자 이상 입력"
                                        value={formData.newPassword}
                                        onChange={(e) => setFormData({ ...formData, newPassword: e.target.value })}
                                    />
                                </div>
                                <div className="space-y-2">
                                    <Label htmlFor="confirmPassword">비밀번호 확인</Label>
                                    <Input
                                        id="confirmPassword"
                                        type="password"
                                        placeholder="비밀번호 다시 입력"
                                        value={formData.confirmPassword}
                                        onChange={(e) => setFormData({ ...formData, confirmPassword: e.target.value })}
                                    />
                                </div>
                                <Button className="w-full" onClick={handleResetPassword} disabled={isLoading}>
                                    {isLoading ? (
                                        <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                                    ) : (
                                        "비밀번호 변경"
                                    )}
                                </Button>
                            </div>
                        )}

                        {step === 'complete' && (
                            <div className="text-center space-y-4">
                                <div className="w-16 h-16 mx-auto rounded-full bg-success/10 flex items-center justify-center">
                                    <CheckCircle2 className="w-8 h-8 text-success" />
                                </div>
                                <p className="text-muted-foreground">
                                    새로운 비밀번호로 로그인해주세요
                                </p>
                                <Button className="w-full" onClick={() => navigate("/login")}>
                                    로그인 페이지로 이동
                                </Button>
                            </div>
                        )}

                        {step !== 'complete' && (
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
                    <Link to="/forgot-id" className="text-sm text-muted-foreground hover:text-primary">
                        아이디가 기억나지 않으세요? <span className="text-primary">아이디 찾기</span>
                    </Link>
                </div>
            </div>
        </div>
    );
};

export default ForgotPassword;
