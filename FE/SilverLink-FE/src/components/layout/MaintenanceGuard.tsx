
import { useMaintenance } from "@/contexts/MaintenanceContext";
import MaintenancePage from "@/pages/MaintenancePage";
import { useLocation } from "react-router-dom";
import { ReactNode } from "react";

export const MaintenanceGuard = ({ children }: { children: ReactNode }) => {
    const { isMaintenanceMode } = useMaintenance();
    const location = useLocation();

    // Always allow access to admin routes, login, and static assets if any
    // Also allow access to the maintenance page itself if we were routing to it,
    // but here we are replacing content, so we just check if we should show it.

    if (isMaintenanceMode) {
        // Allow admin access
        if (location.pathname.startsWith("/admin")) return <>{children}</>;
        // Allow login access
        if (location.pathname.startsWith("/login")) return <>{children}</>;
        // Allow senior login?
        if (location.pathname.startsWith("/senior/login")) return <>{children}</>;

        return <MaintenancePage />;
    }

    return <>{children}</>;
};
