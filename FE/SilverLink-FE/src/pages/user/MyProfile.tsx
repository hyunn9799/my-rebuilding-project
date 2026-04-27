import { useEffect, useState } from 'react';
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { userApi } from "@/api/user";
import { MyProfileResponse, UpdateMyProfileRequest } from "@/types/api";
import { User, Smartphone, Mail, Shield, AlertCircle } from "lucide-react";
import DashboardLayout from "@/components/layout/DashboardLayout";
import { useAuth } from "@/contexts/AuthContext";
import { adminNavItems } from "@/config/adminNavItems";
import { guardianNavItems } from "@/config/guardianNavItems";
import { counselorNavItems } from "@/config/counselorNavItems";

export default function MyProfile() {
    const { user } = useAuth();
    const [profile, setProfile] = useState<MyProfileResponse | null>(null);
    const [loading, setLoading] = useState(false);
    const [submitting, setSubmitting] = useState(false);

    // Determine Nav Items based on Role
    const getNavItems = () => {
        switch (user?.role) {
            case 'ADMIN': return adminNavItems;
            case 'GUARDIAN': return guardianNavItems;
            case 'COUNSELOR': return counselorNavItems;
            default: return [];
        }
    };

    // Form State
    const [formData, setFormData] = useState<UpdateMyProfileRequest>({
        name: '',
        phone: '',
        email: ''
    });

    useEffect(() => {
        loadProfile();
    }, []);

    const loadProfile = async () => {
        setLoading(true);
        try {
            const data = await userApi.getUserProfile();
            setProfile(data);
            setFormData({
                name: data.name,
                phone: data.phone,
                email: data.email
            });
        } catch (error) {
            console.error(error);
            alert("프로필 정보를 불러오지 못했습니다.");
        } finally {
            setLoading(false);
        }
    };

    const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        if (!formData.name || !formData.phone) {
            alert("이름과 전화번호는 필수입니다.");
            return;
        }

        setSubmitting(true);
        try {
            const updated = await userApi.updateUserProfile(formData);
            setProfile(updated);
            alert("프로필이 성공적으로 수정되었습니다.");
        } catch (error) {
            console.error(error);
            alert("프로필 수정 중 오류가 발생했습니다.");
        } finally {
            setSubmitting(false);
        }
    };

    // Loading State wrapped in Layout if user info is available, else just loading
    if (loading && !profile) {
        return (
            <DashboardLayout role={(user?.role?.toLowerCase() || 'guardian') as "admin" | "guardian" | "counselor"} userName={user?.name || '사용자'} navItems={getNavItems()}>
                <div className="flex items-center justify-center h-64">
                    <div className="text-center">로딩 중...</div>
                </div>
            </DashboardLayout>
        );
    }

    if (!profile) {
        return (
            <DashboardLayout role={(user?.role?.toLowerCase() || 'guardian') as "admin" | "guardian" | "counselor"} userName={user?.name || '사용자'} navItems={getNavItems()}>
                <div className="text-center py-8">프로필 정보를 찾을 수 없습니다.</div>
            </DashboardLayout>
        );
    }

    return (
        <DashboardLayout
            role={(user?.role?.toLowerCase() || 'guardian') as "admin" | "guardian" | "counselor"}
            userName={user?.name || profile.name}
            navItems={getNavItems()}
        >
            <div className="container mx-auto p-6 max-w-2xl">

                <h1 className="text-3xl font-bold mb-8">내 프로필</h1>

                <div className="grid gap-6">
                    {/* Basic Info Card (Read-only for sensitive info) */}
                    <Card>
                        <CardHeader>
                            <CardTitle className="text-xl flex items-center gap-2">
                                <Shield className="w-5 h-5 text-blue-600" />
                                계정 정보
                            </CardTitle>
                            <CardDescription>
                                아이디와 권한 등 기본 계정 정보입니다. (수정 불가)
                            </CardDescription>
                        </CardHeader>
                        <CardContent className="space-y-4">
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                <div>
                                    <Label className="text-gray-500">회원번호 (ID)</Label>
                                    <div className="font-medium text-lg">{profile.id}</div>
                                </div>
                                <div>
                                    <Label className="text-gray-500">계정 상태</Label>
                                    <div className="font-medium text-lg">
                                        <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium 
                                            ${profile.status === 'ACTIVE' ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}`}>
                                            {profile.status === 'ACTIVE' ? '활성' : profile.status}
                                        </span>
                                    </div>
                                </div>
                                <div>
                                    <Label className="text-gray-500">역할 (Role)</Label>
                                    <div className="font-medium text-lg">{profile.role}</div>
                                </div>
                                <div>
                                    <Label className="text-gray-500">가입일</Label>
                                    <div className="font-medium text-lg">
                                        {new Date(profile.createdAt).toLocaleDateString()}
                                    </div>
                                </div>
                            </div>
                        </CardContent>
                    </Card>

                    {/* Edit Form */}
                    <Card>
                        <CardHeader>
                            <CardTitle className="text-xl flex items-center gap-2">
                                <User className="w-5 h-5 text-blue-600" />
                                회원 정보 수정
                            </CardTitle>
                            <CardDescription>
                                연락처 및 개인정보를 수정할 수 있습니다.
                            </CardDescription>
                        </CardHeader>
                        <CardContent>
                            <form onSubmit={handleSubmit} className="space-y-4">
                                <div className="space-y-2">
                                    <Label htmlFor="name">이름</Label>
                                    <div className="relative">
                                        <User className="absolute left-3 top-2.5 h-5 w-5 text-gray-400" />
                                        <Input
                                            id="name"
                                            name="name"
                                            value={formData.name}
                                            onChange={handleInputChange}
                                            className="pl-10"
                                            required
                                        />
                                    </div>
                                </div>

                                <div className="space-y-2">
                                    <Label htmlFor="email">이메일</Label>
                                    <div className="relative">
                                        <Mail className="absolute left-3 top-2.5 h-5 w-5 text-gray-400" />
                                        <Input
                                            id="email"
                                            name="email"
                                            type="email"
                                            value={formData.email || ''}
                                            onChange={handleInputChange}
                                            className="pl-10"
                                        />
                                    </div>
                                </div>

                                <div className="space-y-2">
                                    <Label htmlFor="phone">전화번호</Label>
                                    <div className="relative">
                                        <Smartphone className="absolute left-3 top-2.5 h-5 w-5 text-gray-400" />
                                        <Input
                                            id="phone"
                                            name="phone"
                                            value={formData.phone}
                                            onChange={handleInputChange}
                                            className="pl-10"
                                            required
                                        />
                                    </div>
                                    <p className="text-xs text-gray-500 flex items-center gap-1">
                                        <AlertCircle className="w-3 h-3" />
                                        전화번호 변경 시 인증이 필요할 수 있습니다.
                                    </p>
                                </div>

                                <div className="pt-4 flex justify-end">
                                    <Button type="submit" disabled={submitting}>
                                        {submitting ? '저장 중...' : '정보 수정 저장'}
                                    </Button>
                                </div>
                            </form>
                        </CardContent>
                    </Card>
                </div>
            </div>
        </DashboardLayout>
    );
}
