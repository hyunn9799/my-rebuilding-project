import { useEffect, useRef, useCallback } from "react";
import { useNavigate } from "react-router-dom";
import { useToast } from "@/hooks/use-toast";

interface UseSessionTimeoutOptions {
    timeoutMinutes?: number;
    warningMinutes?: number;
    onTimeout?: () => void;
    enabled?: boolean;
}

/**
 * 세션 타임아웃 훅
 * - 지정된 시간 동안 활동이 없으면 자동 로그아웃
 * - 만료 전 경고 토스트 표시
 * - 활동 감지: 마우스 이동, 키보드 입력, 클릭, 터치
 */
const useSessionTimeout = ({
    timeoutMinutes = 30,
    warningMinutes = 5,
    onTimeout,
    enabled = true,
}: UseSessionTimeoutOptions = {}) => {
    const navigate = useNavigate();
    const { toast } = useToast();

    const timeoutRef = useRef<NodeJS.Timeout | null>(null);
    const warningRef = useRef<NodeJS.Timeout | null>(null);
    const warningShownRef = useRef(false);

    const timeoutMs = timeoutMinutes * 60 * 1000;
    const warningMs = (timeoutMinutes - warningMinutes) * 60 * 1000;

    const clearTimers = useCallback(() => {
        if (timeoutRef.current) {
            clearTimeout(timeoutRef.current);
            timeoutRef.current = null;
        }
        if (warningRef.current) {
            clearTimeout(warningRef.current);
            warningRef.current = null;
        }
    }, []);

    const handleLogout = useCallback(() => {
        clearTimers();
        localStorage.removeItem("accessToken");
        localStorage.removeItem("refreshToken");
        localStorage.removeItem("user");

        if (onTimeout) {
            onTimeout();
        } else {
            toast({
                title: "세션 만료",
                description: "장시간 활동이 없어 자동 로그아웃되었습니다.",
                variant: "destructive",
            });
            navigate("/login");
        }
    }, [clearTimers, onTimeout, navigate, toast]);

    const showWarning = useCallback(() => {
        if (!warningShownRef.current) {
            warningShownRef.current = true;
            toast({
                title: "세션 만료 예정",
                description: `${warningMinutes}분 후 자동 로그아웃됩니다. 활동을 계속하시면 연장됩니다.`,
                duration: 10000,
            });
        }
    }, [warningMinutes, toast]);

    const resetTimer = useCallback(() => {
        if (!enabled) return;

        clearTimers();
        warningShownRef.current = false;

        // 만료 경고 타이머
        warningRef.current = setTimeout(showWarning, warningMs);

        // 실제 만료 타이머
        timeoutRef.current = setTimeout(handleLogout, timeoutMs);
    }, [enabled, clearTimers, showWarning, handleLogout, warningMs, timeoutMs]);

    useEffect(() => {
        if (!enabled) {
            clearTimers();
            return;
        }

        const token = localStorage.getItem("accessToken");
        if (!token) {
            return;
        }

        // 활동 감지 이벤트
        const events = ["mousedown", "mousemove", "keydown", "scroll", "touchstart", "click"];

        const handleActivity = () => {
            resetTimer();
        };

        // 이벤트 등록
        events.forEach((event) => {
            document.addEventListener(event, handleActivity, { passive: true });
        });

        // 초기 타이머 시작
        resetTimer();

        return () => {
            clearTimers();
            events.forEach((event) => {
                document.removeEventListener(event, handleActivity);
            });
        };
    }, [enabled, resetTimer, clearTimers]);

    return {
        resetTimer,
        clearTimers,
    };
};

export default useSessionTimeout;
