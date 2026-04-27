import { ReactNode, useEffect } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { Loader2 } from "lucide-react";
import { useAuth } from "@/contexts/AuthContext";

type UserRole = "ADMIN" | "COUNSELOR" | "GUARDIAN" | "ELDERLY";

interface ProtectedRouteProps {
    children: ReactNode;
    allowedRoles?: UserRole[];
    redirectTo?: string;
}

/**
 * 권한 기반 라우트 보호 컴포넌트
 * - AuthContext의 상태를 기반으로 접근 제어
 */
const ProtectedRoute = ({
    children,
    allowedRoles,
    redirectTo = "/login",
}: ProtectedRouteProps) => {
    const location = useLocation();
    const { isLoggedIn, user, isLoading } = useAuth();

    // 로딩 중
    if (isLoading) {
        return (
            <div className="min-h-screen flex items-center justify-center bg-background">
                <div className="text-center space-y-4">
                    <Loader2 className="w-8 h-8 animate-spin mx-auto text-primary" />
                    <p className="text-sm text-muted-foreground">권한을 확인하는 중...</p>
                </div>
            </div>
        );
    }

    // 미인증 시 로그인 페이지로
    if (!isLoggedIn) {
        return (
            <Navigate
                to={redirectTo}
                state={{ from: location.pathname }}
                replace
            />
        );
    }

    // 역할 제한 확인
    if (allowedRoles && allowedRoles.length > 0) {
        // user.role이 allowedRoles에 포함되는지 확인
        if (user && !allowedRoles.includes(user.role as UserRole)) {
            // 권한 없음 -> 홈으로
            return <Navigate to="/" replace />;
        }
    }

    return <>{children}</>;
};

// getRoleHomePath는 AuthContext에서 가져다 쓰는 게 좋으므로 여기서는 제거하거나 재내보내기만 함
// 하지만 App.tsx나 Login.tsx에서 이 파일의 getRoleHomePath를 import하고 있다면 유지해야 함.
// Login.tsx가 ProtectedRoute의 getRoleHomePath를 쓰고 있었음.
// 호환성을 위해 유지하거나 AuthContext에서 import하도록 Login.tsx를 고쳐야 함.
// 일단 여기에 유지하되 AuthContext의 것과 동일하게 구현. (AuthContext와 중복되지만 안전을 위해)
export const getRoleHomePath = (role: UserRole): string => {
    switch (role) {
        case "ADMIN":
            return "/admin";
        case "COUNSELOR":
            return "/counselor";
        case "GUARDIAN":
            return "/guardian";
        case "ELDERLY":
            return "/senior";
        default:
            return "/";
    }
};

export default ProtectedRoute;
