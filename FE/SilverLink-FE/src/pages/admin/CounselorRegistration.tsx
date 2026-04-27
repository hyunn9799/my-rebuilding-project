import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import {
    Select,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
} from "@/components/ui/select";
import { counselorApi } from "@/api/counselor";
import { addressApi } from "@/api/address";
import { AddressResponse, CounselorRequest } from "@/types/api";
import DashboardLayout from "@/components/layout/DashboardLayout";
import { adminNavItems } from "@/config/adminNavItems";
import { UserPlus, Save, ArrowLeft } from "lucide-react";
import { useAuth } from "@/contexts/AuthContext";

export default function CounselorRegistration() {
    const navigate = useNavigate();
    const { user } = useAuth();
    const [isLoading, setIsLoading] = useState(false);

    // Form Data
    const [formData, setFormData] = useState<CounselorRequest>({
        loginId: '',
        password: '',
        name: '',
        email: '',
        phone: '',
        employeeNo: '',
        department: '',
        officePhone: '',
        joinedAt: new Date().toISOString().split('T')[0], // Default to today
        admCode: 0
    });

    // Address State
    const [sidoList, setSidoList] = useState<AddressResponse[]>([]);
    const [sigunguList, setSigunguList] = useState<AddressResponse[]>([]);
    const [dongList, setDongList] = useState<AddressResponse[]>([]);

    const [selectedSido, setSelectedSido] = useState<string>('');
    const [selectedSigungu, setSelectedSigungu] = useState<string>('');
    const [selectedDong, setSelectedDong] = useState<string>('');

    // Load Sido on Mount
    useEffect(() => {
        loadSido();
    }, []);

    const loadSido = async () => {
        try {
            const data = await addressApi.getSido();
            // 중복 제거: sidoCode 기준으로 유니크하게
            const uniqueData = data.filter((item, index, self) => 
                index === self.findIndex((t) => t.sidoCode === item.sidoCode)
            );
            setSidoList(uniqueData);
        } catch (error) {
            console.error("Failed to load Sido:", error);
        }
    };

    // Load Sigungu when Sido changes
    useEffect(() => {
        if (selectedSido) {
            loadSigungu(selectedSido);
            setSigunguList([]);
            setDongList([]);
            setSelectedSigungu('');
            setSelectedDong('');
        }
    }, [selectedSido]);

    const loadSigungu = async (sidoCode: string) => {
        try {
            const data = await addressApi.getSigungu(sidoCode);
            // 중복 제거: sigunguCode 기준으로 유니크하게
            const uniqueData = data.filter((item, index, self) => 
                index === self.findIndex((t) => t.sigunguCode === item.sigunguCode)
            );
            setSigunguList(uniqueData);
        } catch (error) {
            console.error("Failed to load Sigungu:", error);
        }
    };

    // Load Dong when Sigungu changes
    useEffect(() => {
        if (selectedSido && selectedSigungu) {
            loadDong(selectedSido, selectedSigungu);
            setDongList([]);
            setSelectedDong('');
        }
    }, [selectedSigungu]);

    const loadDong = async (sidoCode: string, sigunguCode: string) => {
        try {
            const data = await addressApi.getDong(sidoCode, sigunguCode);
            // 중복 제거: admCode 기준으로 유니크하게
            const uniqueData = data.filter((item, index, self) => 
                index === self.findIndex((t) => t.admCode === item.admCode)
            );
            setDongList(uniqueData);
        } catch (error) {
            console.error("Failed to load Dong:", error);
        }
    };

    // Set admCode when Dong changes
    useEffect(() => {
        if (selectedDong) {
            const dong = dongList.find(d => d.dongName === selectedDong); // Assuming dongName is unique within sigungu or use ID if available
            // Wait, selectedDong is the value from Select. Let's use the code/name carefully.
            // The AddressResponse has admCode. We should store admCode in value if possible, or lookup.
            // Let's make Select value the index or find object.
            // Better: Set Select value to admCode directly if possible?
            // But Dong list items have separate admCodes.
            // Let's assume select value is stringified admCode for Dong.
        }
    }, [selectedDong]);


    const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
    };

    const handleAddressChange = (level: 'sido' | 'sigungu' | 'dong', value: string) => {
        if (level === 'sido') {
            // value is sidoCode
            setSelectedSido(value);
        } else if (level === 'sigungu') {
            // value is sigunguCode
            setSelectedSigungu(value);
        } else if (level === 'dong') {
            // value is admCode (as string)
            setSelectedDong(value);
            setFormData(prev => ({ ...prev, admCode: Number(value) }));
        }
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setIsLoading(true);

        try {
            await counselorApi.registerCounselor(formData);
            alert("상담사가 성공적으로 등록되었습니다.");
            navigate('/admin/members'); // List page is MemberManagement
        } catch (error: any) {
            console.error(error);
            const msg = error.response?.data?.message || "상담사 등록 중 오류가 발생했습니다.";
            alert(msg);
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <DashboardLayout role="admin" userName={user?.name || "관리자"} navItems={adminNavItems}>
            <div className="container mx-auto p-6 max-w-3xl">
                <div className="flex items-center gap-4 mb-6">

                    <h1 className="text-2xl font-bold flex items-center gap-2">
                        <UserPlus className="w-6 h-6 text-primary" />
                        상담사 등록
                    </h1>
                </div>

                <form onSubmit={handleSubmit}>
                    <Card className="mb-6">
                        <CardHeader>
                            <CardTitle>계정 정보</CardTitle>
                            <CardDescription>상담사가 사용할 로그인 계정 정보입니다.</CardDescription>
                        </CardHeader>
                        <CardContent className="grid gap-4">
                            <div className="grid grid-cols-2 gap-4">
                                <div className="space-y-2">
                                    <Label htmlFor="loginId">아이디 <span className="text-red-500">*</span></Label>
                                    <Input id="loginId" name="loginId" value={formData.loginId} onChange={handleInputChange} required />
                                </div>
                                <div className="space-y-2">
                                    <Label htmlFor="password">비밀번호 <span className="text-red-500">*</span></Label>
                                    <Input id="password" name="password" type="password" value={formData.password} onChange={handleInputChange} required />
                                </div>
                            </div>
                            <div className="grid grid-cols-2 gap-4">
                                <div className="space-y-2">
                                    <Label htmlFor="name">이름 <span className="text-red-500">*</span></Label>
                                    <Input id="name" name="name" value={formData.name} onChange={handleInputChange} required />
                                </div>
                                <div className="space-y-2">
                                    <Label htmlFor="phone">휴대전화 <span className="text-red-500">*</span></Label>
                                    <Input id="phone" name="phone" value={formData.phone} onChange={handleInputChange} required placeholder="010-0000-0000" />
                                </div>
                            </div>
                            <div className="space-y-2">
                                <Label htmlFor="email">이메일</Label>
                                <Input id="email" name="email" type="email" value={formData.email} onChange={handleInputChange} />
                            </div>
                        </CardContent>
                    </Card>

                    <Card className="mb-6">
                        <CardHeader>
                            <CardTitle>근무 및 담당 지역 정보</CardTitle>
                            <CardDescription>상담사의 근무 상세 정보와 담당 행정 구역입니다.</CardDescription>
                        </CardHeader>
                        <CardContent className="grid gap-4">
                            <div className="grid grid-cols-2 gap-4">
                                <div className="space-y-2">
                                    <Label htmlFor="department">부서</Label>
                                    <Input id="department" name="department" value={formData.department} onChange={handleInputChange} />
                                </div>
                                <div className="space-y-2">
                                    <Label htmlFor="employeeNo">사번</Label>
                                    <Input id="employeeNo" name="employeeNo" value={formData.employeeNo} onChange={handleInputChange} />
                                </div>
                            </div>
                            <div className="grid grid-cols-2 gap-4">
                                <div className="space-y-2">
                                    <Label htmlFor="officePhone">사무실 전화</Label>
                                    <Input id="officePhone" name="officePhone" value={formData.officePhone} onChange={handleInputChange} />
                                </div>
                                <div className="space-y-2">
                                    <Label htmlFor="joinedAt">입사일</Label>
                                    <Input id="joinedAt" name="joinedAt" type="date" value={formData.joinedAt} onChange={handleInputChange} />
                                </div>
                            </div>

                            <div className="space-y-2">
                                <Label>담당 행정 구역 (필수) <span className="text-red-500">*</span></Label>
                                <div className="grid grid-cols-3 gap-2">
                                    <Select value={selectedSido} onValueChange={(val) => handleAddressChange('sido', val)}>
                                        <SelectTrigger>
                                            <SelectValue placeholder="시/도 선택" />
                                        </SelectTrigger>
                                        <SelectContent>
                                            {sidoList.map((sido, index) => (
                                                <SelectItem key={`sido-${index}`} value={sido.sidoCode || ''}>
                                                    {sido.sidoName}
                                                </SelectItem>
                                            ))}
                                        </SelectContent>
                                    </Select>

                                    <Select value={selectedSigungu} onValueChange={(val) => handleAddressChange('sigungu', val)} disabled={!selectedSido}>
                                        <SelectTrigger>
                                            <SelectValue placeholder="시/군/구 선택" />
                                        </SelectTrigger>
                                        <SelectContent>
                                            {sigunguList.map((sigungu, index) => (
                                                <SelectItem key={`sigungu-${index}`} value={sigungu.sigunguCode || ''}>
                                                    {sigungu.sigunguName}
                                                </SelectItem>
                                            ))}
                                        </SelectContent>
                                    </Select>

                                    <Select value={selectedDong} onValueChange={(val) => handleAddressChange('dong', val)} disabled={!selectedSigungu}>
                                        <SelectTrigger>
                                            <SelectValue placeholder="읍/면/동 선택" />
                                        </SelectTrigger>
                                        <SelectContent>
                                            {dongList.map((dong, index) => (
                                                <SelectItem key={`dong-${index}`} value={String(dong.admCode)}>
                                                    {dong.dongName}
                                                </SelectItem>
                                            ))}
                                        </SelectContent>
                                    </Select>
                                </div>
                                <p className="text-sm text-gray-500 mt-1">
                                    선택된 행정 코드: {formData.admCode || '미선택'}
                                </p>
                            </div>
                        </CardContent>
                    </Card>

                    <div className="flex justify-end gap-2">
                        <Button type="button" variant="outline" onClick={() => navigate(-1)}>취소</Button>
                        <Button type="submit" disabled={isLoading || !formData.admCode || !formData.loginId || !formData.password || !formData.name || !formData.phone}>
                            {isLoading ? '저장 중...' : '상담사 등록 저장'}
                        </Button>
                    </div>
                </form>
            </div>
        </DashboardLayout>
    );
}
