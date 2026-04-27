import { useState, useEffect, useCallback } from 'react';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Phone, CheckCircle2, Loader2, RefreshCw } from 'lucide-react';
import { requestVerificationCode, verifyCode, PhoneVerificationPurpose } from '@/api/phoneVerification';
import { useToast } from '@/hooks/use-toast';

interface PhoneVerificationProps {
    phone: string;
    purpose?: PhoneVerificationPurpose;
    onVerified: (proofToken: string) => void;
    disabled?: boolean;
}

const PhoneVerification = ({
    phone,
    purpose = 'SIGNUP',
    onVerified,
    disabled = false,
}: PhoneVerificationProps) => {
    const { toast } = useToast();
    const [verificationId, setVerificationId] = useState<number | null>(null);
    const [code, setCode] = useState('');
    const [loading, setLoading] = useState(false);
    const [verifying, setVerifying] = useState(false);
    const [verified, setVerified] = useState(false);
    const [cooldown, setCooldown] = useState(0);
    const [expireTime, setExpireTime] = useState(0);

    // 쿨다운 타이머
    useEffect(() => {
        if (cooldown <= 0) return;
        const timer = setInterval(() => {
            setCooldown((prev) => Math.max(0, prev - 1));
        }, 1000);
        return () => clearInterval(timer);
    }, [cooldown]);

    // 만료 타이머
    useEffect(() => {
        if (expireTime <= 0 || verified) return;
        const timer = setInterval(() => {
            setExpireTime((prev) => {
                const next = Math.max(0, prev - 1);
                if (next === 0) {
                    toast({
                        variant: 'destructive',
                        title: '인증 시간 만료',
                        description: '인증번호가 만료되었습니다. 다시 요청해주세요.',
                    });
                    setVerificationId(null);
                    setCode('');
                }
                return next;
            });
        }, 1000);
        return () => clearInterval(timer);
    }, [expireTime, verified, toast]);

    const handleRequestCode = useCallback(async () => {
        if (!phone || phone.length < 10) {
            toast({
                variant: 'destructive',
                title: '전화번호 오류',
                description: '올바른 전화번호를 입력해주세요.',
            });
            return;
        }

        setLoading(true);
        try {
            const response = await requestVerificationCode(phone, purpose);
            setVerificationId(response.verificationId);
            setCooldown(60); // 60초 재발송 쿨다운
            setExpireTime(180); // 3분 만료
            setCode('');
            setVerified(false);

            toast({
                title: '인증번호 발송',
                description: '휴대폰으로 인증번호가 발송되었습니다.',
            });
        } catch (error: any) {
            let message = '인증번호 발송에 실패했습니다.';
            if (error.response?.data?.message) {
                const errorCode = error.response.data.message;
                if (errorCode === 'PHONE_COOLDOWN') {
                    message = '잠시 후 다시 시도해주세요.';
                } else if (errorCode === 'PHONE_DAILY_LIMIT') {
                    message = '일일 발송 한도를 초과했습니다.';
                }
            }
            toast({
                variant: 'destructive',
                title: '발송 실패',
                description: message,
            });
        } finally {
            setLoading(false);
        }
    }, [phone, purpose, toast]);

    const handleVerifyCode = useCallback(async () => {
        if (!verificationId || !code) return;

        setVerifying(true);
        try {
            const response = await verifyCode(verificationId, code);
            if (response.verified) {
                setVerified(true);
                onVerified(response.proofToken);
                toast({
                    title: '인증 완료',
                    description: '휴대폰 인증이 완료되었습니다.',
                });
            }
        } catch (error: any) {
            let message = '인증에 실패했습니다.';
            if (error.response?.data?.message) {
                const errorCode = error.response.data.message;
                if (errorCode === 'PV_CODE_INVALID') {
                    message = '인증번호가 일치하지 않습니다.';
                } else if (errorCode === 'PV_EXPIRED') {
                    message = '인증번호가 만료되었습니다. 다시 요청해주세요.';
                    setVerificationId(null);
                    setCode('');
                } else if (errorCode === 'PV_TOO_MANY_ATTEMPTS') {
                    message = '시도 횟수를 초과했습니다. 다시 요청해주세요.';
                    setVerificationId(null);
                    setCode('');
                }
            }
            toast({
                variant: 'destructive',
                title: '인증 실패',
                description: message,
            });
        } finally {
            setVerifying(false);
        }
    }, [verificationId, code, onVerified, toast]);

    const formatTime = (seconds: number) => {
        const mins = Math.floor(seconds / 60);
        const secs = seconds % 60;
        return `${mins}:${secs.toString().padStart(2, '0')}`;
    };

    if (verified) {
        return (
            <div className="flex items-center gap-2 p-3 rounded-lg bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800">
                <CheckCircle2 className="w-5 h-5 text-green-600" />
                <span className="text-sm font-medium text-green-700 dark:text-green-400">
                    휴대폰 인증 완료
                </span>
            </div>
        );
    }

    return (
        <div className="space-y-3">
            <div className="flex items-center gap-2">
                <Button
                    type="button"
                    variant="outline"
                    onClick={handleRequestCode}
                    disabled={disabled || loading || cooldown > 0 || !phone}
                    className="whitespace-nowrap"
                >
                    {loading ? (
                        <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                    ) : cooldown > 0 ? (
                        <RefreshCw className="w-4 h-4 mr-2" />
                    ) : (
                        <Phone className="w-4 h-4 mr-2" />
                    )}
                    {loading
                        ? '발송 중...'
                        : cooldown > 0
                            ? `재발송 (${cooldown}초)`
                            : verificationId
                                ? '재발송'
                                : '인증번호 발송'}
                </Button>
                {expireTime > 0 && !verified && (
                    <Badge variant="secondary" className="text-orange-600 bg-orange-100">
                        남은시간 {formatTime(expireTime)}
                    </Badge>
                )}
            </div>

            {verificationId && (
                <div className="flex items-center gap-2">
                    <Input
                        type="text"
                        placeholder="인증번호 6자리 입력"
                        value={code}
                        onChange={(e) => setCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
                        maxLength={6}
                        className="w-40"
                        disabled={verified}
                    />
                    <Button
                        type="button"
                        onClick={handleVerifyCode}
                        disabled={code.length < 6 || verifying || verified}
                    >
                        {verifying ? <Loader2 className="w-4 h-4 animate-spin" /> : '확인'}
                    </Button>
                </div>
            )}
        </div>
    );
};

export default PhoneVerification;
