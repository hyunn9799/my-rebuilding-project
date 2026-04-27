import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { toast } from "sonner";
import {
  Heart,
  Fingerprint,
  Shield,
  ArrowLeft,
  Loader2,
  CheckCircle2,
  AlertCircle,
} from "lucide-react";
import { useWebAuthn, isWebAuthnSupported } from "@/hooks/useWebAuthn";

const SeniorBiometric = () => {
  const navigate = useNavigate();
  const [showRegistrationDialog, setShowRegistrationDialog] = useState(false);
  const [registrationName, setRegistrationName] = useState("");

  const {
    isPlatformAvailable,
    isRegistering,
    error,
    register,
    checkPlatformAuthenticator,
  } = useWebAuthn();

  useEffect(() => {
    checkPlatformAuthenticator();
  }, [checkPlatformAuthenticator]);

  // 지문 등록 (로그인 후에만 가능)
  // ✅ 보안 강화: userId는 JWT에서 자동 추출되므로 전달하지 않음
  const handleBiometricRegistration = async () => {
    if (!registrationName.trim()) {
      toast.error("이름을 입력해주세요.");
      return;
    }

    const success = await register();  // ✅ userId 파라미터 제거
    if (success) {
      toast.success("지문 등록 완료!", {
        description: "이제 지문으로 로그인할 수 있어요.",
      });
      setShowRegistrationDialog(false);
      setRegistrationName("");
    } else if (error) {
      toast.error(error);
    }
  };

  const webAuthnSupport = isWebAuthnSupported();
  const showBiometric = webAuthnSupport && isPlatformAvailable;

  return (
    <div className="min-h-screen bg-gradient-to-b from-primary/10 to-background">
      {/* Header */}
      <header className="p-4 sm:p-6">
        <div className="flex items-center gap-3">
          <Button
            variant="ghost"
            size="icon"
            className="w-12 h-12"
            onClick={() => navigate("/senior")}
          >
            <ArrowLeft className="w-6 h-6" />
          </Button>
          <div className="flex items-center gap-3">
            <div className="w-12 h-12 sm:w-16 sm:h-16 rounded-2xl bg-primary flex items-center justify-center">
              <Heart className="w-7 h-7 sm:w-10 sm:h-10 text-primary-foreground" />
            </div>
            <div>
              <h1 className="text-2xl sm:text-3xl font-bold text-foreground">지문 등록</h1>
              <p className="text-sm sm:text-base text-muted-foreground">간편 로그인 설정</p>
            </div>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="flex-1 p-4 sm:p-6">
        <div className="w-full max-w-2xl mx-auto space-y-5">
          {/* Biometric Security Card */}
          {showBiometric && (
            <Card className="shadow-lg border-2 border-primary/30">
              <CardHeader>
                <div className="flex items-center gap-3">
                  <div className="w-14 h-14 rounded-xl bg-primary/10 flex items-center justify-center">
                    <Shield className="w-7 h-7 text-primary" />
                  </div>
                  <div>
                    <CardTitle className="text-lg sm:text-xl">지문 인증</CardTitle>
                    <CardDescription className="text-base">
                      지문으로 더 안전하고 빠르게
                    </CardDescription>
                  </div>
                </div>
              </CardHeader>
              <CardContent className="space-y-4">
                <div className="p-4 bg-primary/5 rounded-lg border border-primary/20">
                  <div className="flex items-start gap-3">
                    <Fingerprint className="w-6 h-6 text-primary mt-1" />
                    <div className="flex-1">
                      <h3 className="font-bold text-lg mb-1">지문 인증이란?</h3>
                      <p className="text-muted-foreground text-base">
                        지문을 등록하면 비밀번호 없이 빠르고 안전하게 로그인할 수 있어요.
                        휴대폰에 저장된 지문으로 간편하게 로그인하세요.
                      </p>
                    </div>
                  </div>
                </div>

                <div className="p-4 bg-blue-50 rounded-lg border border-blue-200">
                  <h3 className="font-bold text-lg mb-2 text-blue-900">지문 등록 방법</h3>
                  <ol className="space-y-2 text-base text-blue-800">
                    <li className="flex gap-2">
                      <span className="font-bold">1.</span>
                      <span>아래 "지문 등록하기" 버튼을 누르세요</span>
                    </li>
                    <li className="flex gap-2">
                      <span className="font-bold">2.</span>
                      <span>이름을 입력하세요</span>
                    </li>
                    <li className="flex gap-2">
                      <span className="font-bold">3.</span>
                      <span>휴대폰의 지문 센서에 손가락을 대세요</span>
                    </li>
                    <li className="flex gap-2">
                      <span className="font-bold">4.</span>
                      <span>등록 완료! 다음부터 지문으로 로그인하세요</span>
                    </li>
                  </ol>
                </div>

                <Button
                  onClick={() => setShowRegistrationDialog(true)}
                  className="w-full h-16 text-xl font-bold rounded-xl"
                  size="lg"
                >
                  <Fingerprint className="w-6 h-6 mr-3" />
                  지문 등록하기
                </Button>

                <div className="flex items-center gap-2 text-success justify-center">
                  <CheckCircle2 className="w-4 h-4" />
                  <span className="text-sm">이 기기는 생체 인증을 지원해요</span>
                </div>
              </CardContent>
            </Card>
          )}

          {/* WebAuthn Not Supported */}
          {!showBiometric && (
            <Card className="shadow-md">
              <CardContent className="p-6">
                <div className="flex flex-col items-center justify-center gap-4 text-center">
                  <AlertCircle className="w-12 h-12 text-muted-foreground" />
                  <div>
                    <h3 className="font-bold text-xl mb-2">지문 인증을 사용할 수 없어요</h3>
                    <p className="text-base text-muted-foreground">
                      이 기기는 지문 인증을 지원하지 않습니다.
                      <br />
                      휴대폰 인증으로 로그인해주세요.
                    </p>
                  </div>
                </div>
              </CardContent>
            </Card>
          )}
        </div>
      </main>

      {/* Registration Dialog */}
      <Dialog open={showRegistrationDialog} onOpenChange={setShowRegistrationDialog}>
        <DialogContent className="max-w-sm mx-auto">
          <DialogHeader className="text-center">
            <div className="mx-auto w-20 h-20 rounded-full bg-primary/10 flex items-center justify-center mb-4">
              <Fingerprint className="w-10 h-10 text-primary" />
            </div>
            <DialogTitle className="text-2xl">지문 등록</DialogTitle>
            <DialogDescription className="text-lg">
              이름을 입력하고 지문을 등록하세요
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="dialogName" className="text-lg">이름</Label>
              <Input
                id="dialogName"
                placeholder="이름을 입력하세요"
                value={registrationName}
                onChange={(e) => setRegistrationName(e.target.value)}
                className="h-14 text-lg"
              />
            </div>
          </div>

          <DialogFooter className="flex flex-col gap-3 sm:flex-col">
            <Button
              onClick={handleBiometricRegistration}
              className="w-full h-16 text-lg font-bold"
              disabled={isRegistering || !registrationName.trim()}
            >
              {isRegistering ? (
                <>
                  <Loader2 className="w-6 h-6 mr-3 animate-spin" />
                  등록 중...
                </>
              ) : (
                <>
                  <Fingerprint className="w-6 h-6 mr-3" />
                  지문 등록하기
                </>
              )}
            </Button>
            <Button
              variant="outline"
              onClick={() => setShowRegistrationDialog(false)}
              className="w-full h-14 text-lg"
            >
              취소
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
};

export default SeniorBiometric;
