import { createContext, useContext, useState, ReactNode, useEffect, useCallback } from 'react';
import { SessionExpiredDialog } from '@/components/auth/SessionExpiredDialog';
import { setAccessToken } from '@/api';

interface SessionExpiredContextType {
    showSessionExpiredDialog: () => void;
}

const SessionExpiredContext = createContext<SessionExpiredContextType | undefined>(undefined);

export const SessionExpiredProvider = ({ children }: { children: ReactNode }) => {
    const [isOpen, setIsOpen] = useState(false);

    const showSessionExpiredDialog = useCallback(() => {
        setIsOpen(true);
    }, []);

    const handleConfirm = () => {
        setIsOpen(false);
        // 토큰 제거 및 로그인 페이지로 이동
        setAccessToken(null);
        window.location.href = '/login';
    };

    // 전역 핸들러 등록
    useEffect(() => {
        setGlobalSessionExpiredHandler(showSessionExpiredDialog);
        return () => {
            setGlobalSessionExpiredHandler(() => { });
        };
    }, [showSessionExpiredDialog]);

    return (
        <SessionExpiredContext.Provider value={{ showSessionExpiredDialog }}>
            {children}
            <SessionExpiredDialog open={isOpen} onConfirm={handleConfirm} />
        </SessionExpiredContext.Provider>
    );
};

export const useSessionExpired = () => {
    const context = useContext(SessionExpiredContext);
    if (!context) {
        throw new Error('useSessionExpired must be used within SessionExpiredProvider');
    }
    return context;
};

// API 인터셉터에서 사용할 전역 함수
let globalShowSessionExpired: (() => void) | null = null;

export const setGlobalSessionExpiredHandler = (handler: () => void) => {
    globalShowSessionExpired = handler;
};

export const showGlobalSessionExpiredDialog = () => {
    if (globalShowSessionExpired) {
        globalShowSessionExpired();
    } else {
        // Fallback - Context가 없을 때 기본 동작
        alert('세션이 만료되었습니다. 다시 로그인해주세요.');
        window.location.href = '/login';
    }
};
