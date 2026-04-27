import { createContext, useContext, useState, ReactNode, useEffect, useCallback } from 'react';
import { SessionInvalidatedDialog } from '@/components/auth/SessionInvalidatedDialog';
import { setAccessToken } from '@/api';

interface SessionInvalidatedContextType {
    showSessionInvalidatedDialog: () => void;
}

const SessionInvalidatedContext = createContext<SessionInvalidatedContextType | undefined>(undefined);

export const SessionInvalidatedProvider = ({ children }: { children: ReactNode }) => {
    const [isOpen, setIsOpen] = useState(false);

    const showSessionInvalidatedDialog = useCallback(() => {
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
        setGlobalSessionInvalidatedHandler(showSessionInvalidatedDialog);
        return () => {
            setGlobalSessionInvalidatedHandler(() => { });
        };
    }, [showSessionInvalidatedDialog]);

    return (
        <SessionInvalidatedContext.Provider value={{ showSessionInvalidatedDialog }}>
            {children}
            <SessionInvalidatedDialog open={isOpen} onConfirm={handleConfirm} />
        </SessionInvalidatedContext.Provider>
    );
};

export const useSessionInvalidated = () => {
    const context = useContext(SessionInvalidatedContext);
    if (!context) {
        throw new Error('useSessionInvalidated must be used within SessionInvalidatedProvider');
    }
    return context;
};

// API 인터셉터에서 사용할 전역 함수
let globalShowSessionInvalidated: (() => void) | null = null;

export const setGlobalSessionInvalidatedHandler = (handler: () => void) => {
    globalShowSessionInvalidated = handler;
};

export const showGlobalSessionInvalidatedDialog = () => {
    if (globalShowSessionInvalidated) {
        globalShowSessionInvalidated();
    } else {
        // Fallback - Context가 없을 때 기본 동작
        alert('다른 기기에서 로그인하여 현재 세션이 종료되었습니다.');
        window.location.href = '/login';
    }
};
