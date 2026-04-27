
import { createContext, useContext, useState, useEffect, ReactNode } from "react";

interface MaintenanceContextType {
    isMaintenanceMode: boolean;
    setMaintenanceMode: (value: boolean) => void;
}

const MaintenanceContext = createContext<MaintenanceContextType | undefined>(undefined);

export const MaintenanceProvider = ({ children }: { children: ReactNode }) => {
    const [isMaintenanceMode, setIsMaintenanceMode] = useState(() => {
        const saved = localStorage.getItem("maintenanceMode");
        return saved === "true";
    });

    useEffect(() => {
        localStorage.setItem("maintenanceMode", String(isMaintenanceMode));
    }, [isMaintenanceMode]);

    return (
        <MaintenanceContext.Provider value={{ isMaintenanceMode, setMaintenanceMode: setIsMaintenanceMode }}>
            {children}
        </MaintenanceContext.Provider>
    );
};

export const useMaintenance = () => {
    const context = useContext(MaintenanceContext);
    if (context === undefined) {
        throw new Error("useMaintenance must be used within a MaintenanceProvider");
    }
    return context;
};
