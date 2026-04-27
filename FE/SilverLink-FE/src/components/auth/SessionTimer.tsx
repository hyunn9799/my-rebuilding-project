import { useState, useEffect, useMemo } from 'react';
import { Clock, RefreshCw, AlertTriangle } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { toast } from 'sonner';
import { refresh as refreshToken, getSessionInfo } from '@/api/auth';

interface SessionTimerProps {
  onSessionExpired?: () => void;
}

export const SessionTimer = ({ onSessionExpired }: SessionTimerProps) => {
  const [remainingSeconds, setRemainingSeconds] = useState<number | null>(null);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [showWarning, setShowWarning] = useState(false);
  const [lastRefresh, setLastRefresh] = useState(0);

  // 세션 정보 가져오기
  const fetchSessionInfo = async () => {
    try {
      const info = await getSessionInfo();
      setRemainingSeconds(info.remainingSeconds);
    } catch (error) {
      console.error('Failed to fetch session info:', error);
    }
  };

  // 초기 로드
  useEffect(() => {
    fetchSessionInfo();
  }, []);

  // 1초마다 카운트다운
  useEffect(() => {
    if (remainingSeconds === null) return;

    const timer = setInterval(() => {
      setRemainingSeconds((prev) => {
        if (prev === null || prev <= 0) {
          clearInterval(timer);
          onSessionExpired?.();
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(timer);
  }, [remainingSeconds, onSessionExpired]);

  // 5분 전 경고
  useEffect(() => {
    if (remainingSeconds !== null && remainingSeconds <= 300 && remainingSeconds > 0) {
      if (!showWarning) {
        setShowWarning(true);
        toast.warning('세션 만료 임박', {
          description: '5분 후 자동 로그아웃됩니다. 연장하시겠습니까?',
        });
      }
    } else {
      setShowWarning(false);
    }
  }, [remainingSeconds, showWarning]);

  // 세션 연장
  const handleExtendSession = async () => {
    // 연장 제한 (1분에 1회)
    const now = Date.now();
    if (now - lastRefresh < 60000) {
      toast.error('잠시 후 다시 시도해주세요');
      return;
    }

    setIsRefreshing(true);
    try {
      await refreshToken();
      await fetchSessionInfo();
      setLastRefresh(now);
      toast.success('세션 연장 완료', {
        description: '세션이 60분 연장되었습니다.',
      });
    } catch (error) {
      toast.error('세션 연장 실패', {
        description: '다시 시도해주세요.',
      });
    } finally {
      setIsRefreshing(false);
    }
  };

  // 시간 포맷팅
  const formatTime = (seconds: number) => {
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = seconds % 60;

    if (hours > 0) {
      return `${hours}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    }
    return `${minutes}:${secs.toString().padStart(2, '0')}`;
  };

  // 포맷팅 결과 캐싱
  const formattedTime = useMemo(() => {
    if (remainingSeconds === null) return '';
    return formatTime(remainingSeconds);
  }, [remainingSeconds]);

  if (remainingSeconds === null) return null;

  return (
    <div className="flex items-center gap-2">
      {/* 타이머 표시 */}
      <Badge
        variant={showWarning ? 'destructive' : 'secondary'}
        className="flex items-center gap-1.5 px-3 py-1.5"
        role="timer"
        aria-live="polite"
        aria-label={`세션 남은 시간: ${formattedTime}`}
      >
        {showWarning ? (
          <>
            <AlertTriangle className="w-3.5 h-3.5 animate-pulse" />
            <span className="sr-only">경고: 세션이 5분 후 만료됩니다</span>
          </>
        ) : (
          <Clock className="w-3.5 h-3.5" />
        )}
        <span className="font-mono text-sm">
          {formattedTime}
        </span>
      </Badge>

      {/* 연장 버튼 */}
      <Button
        variant="ghost"
        size="sm"
        onClick={handleExtendSession}
        disabled={isRefreshing}
        className="h-8 hidden sm:flex"
        aria-label="세션 연장"
        aria-busy={isRefreshing}
      >
        {isRefreshing ? (
          <div className="w-4 h-4 border-2 border-muted-foreground/30 border-t-muted-foreground rounded-full animate-spin" />
        ) : (
          <RefreshCw className="w-4 h-4" />
        )}
        <span className="ml-1.5">연장</span>
      </Button>
    </div>
  );
};
