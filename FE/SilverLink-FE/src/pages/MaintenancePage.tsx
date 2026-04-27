
import { AlertTriangle, Clock } from "lucide-react";
import { Button } from "@/components/ui/button";
import { useNavigate } from "react-router-dom";

const MaintenancePage = () => {
    const navigate = useNavigate();

    return (
        <div className="min-h-screen bg-gray-50 flex flex-col items-center justify-center p-4">
            <div className="max-w-md w-full bg-white rounded-lg shadow-lg p-8 text-center space-y-6">
                <div className="flex justify-center">
                    <div className="w-20 h-20 bg-yellow-100 rounded-full flex items-center justify-center">
                        <AlertTriangle className="w-10 h-10 text-yellow-600" />
                    </div>
                </div>

                <div className="space-y-2">
                    <h1 className="text-2xl font-bold text-gray-900">서비스 점검중입니다</h1>
                    <p className="text-gray-600">
                        보다 나은 서비스 제공을 위해 시스템 점검을 진행하고 있습니다.
                        이용에 불편을 드려 죄송합니다.
                    </p>
                </div>

                <div className="bg-gray-50 rounded-lg p-4 space-y-3">
                    <div className="flex items-center justify-center gap-2 text-sm text-gray-600">
                        <Clock className="w-4 h-4" />
                        <span>점검 시간</span>
                    </div>
                    <p className="font-medium text-gray-900">
                        2026년 01월 30일 14:00 ~ 16:00
                    </p>
                </div>

                <div className="pt-4">
                    <Button
                        variant="outline"
                        onClick={() => navigate("/login")}
                        className="w-full"
                    >
                        관리자 로그인
                    </Button>
                </div>
            </div>

            <div className="mt-8 text-center text-sm text-gray-500">
                <p>문의사항: support@maumdolbom.go.kr</p>
                <p>전화: 1588-0000</p>
            </div>
        </div>
    );
};

export default MaintenancePage;
