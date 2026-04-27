import { useState, useEffect } from "react";
import { User, Phone, Mail, Shield, Edit2, Save, X, Loader2, MapPin, Building } from "lucide-react";
import DashboardLayout from "@/components/layout/DashboardLayout";
import { adminNavItems } from "@/config/adminNavItems";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { useToast } from "@/hooks/use-toast";
import usersApi from "@/api/users";
import adminsApi from "@/api/admins";
import { MyProfileResponse, AdminResponse } from "@/types/api";
import { useAuth } from "@/contexts/AuthContext";

const AdminProfile = () => {
    const { toast } = useToast();
    const { user } = useAuth();
    const [isLoading, setIsLoading] = useState(true);
    const [isEditing, setIsEditing] = useState(false);
    const [isSaving, setIsSaving] = useState(false);
    const [profile, setProfile] = useState<MyProfileResponse | null>(null);
    const [adminInfo, setAdminInfo] = useState<AdminResponse | null>(null);

    // 수정용 폼 상태
    const [formData, setFormData] = useState({
        name: "",
        phone: "",
    });

    useEffect(() => {
        fetchProfile();
    }, []);

    const fetchProfile = async () => {
        try {
            setIsLoading(true);
            const [userProfile, adminData] = await Promise.allSettled([
                usersApi.getMyProfile(),
                adminsApi.getMyInfo(),
            ]);

            if (userProfile.status === 'fulfilled') {
                setProfile(userProfile.value);
                setFormData({
                    name: userProfile.value.name || "",
                    phone: userProfile.value.phone || "",
                });
            }

            if (adminData.status === 'fulfilled') {
                setAdminInfo(adminData.value);
            }
        } catch (error) {
            console.error("Failed to fetch profile:", error);
            toast({
                title: "오류",
                description: "프로필 정보를 불러오는데 실패했습니다.",
                variant: "destructive",
            });
        } finally {
            setIsLoading(false);
        }
    };

    const handleSave = async () => {
        try {
            setIsSaving(true);
            await usersApi.updateMyProfile({
                name: formData.name,
                phone: formData.phone,
            });
            toast({
                title: "저장 완료",
                description: "프로필이 성공적으로 수정되었습니다.",
            });
            setIsEditing(false);
            fetchProfile();
        } catch (error) {
            console.error("Failed to save profile:", error);
            toast({
                title: "저장 실패",
                description: "프로필 수정에 실패했습니다.",
                variant: "destructive",
            });
        } finally {
            setIsSaving(false);
        }
    };

    const handleCancel = () => {
        setFormData({
            name: profile?.name || "",
            phone: profile?.phone || "",
        });
        setIsEditing(false);
    };

    const getLevelName = (level?: string) => {
        switch (level) {
            case 'NATIONAL': return '전국';
            case 'PROVINCIAL': return '시/도';
            case 'CITY': return '시/군/구';
            case 'DISTRICT': return '읍/면/동';
            default: return level || '-';
        }
    };

    if (isLoading) {
        return (
            <DashboardLayout role="admin" userName={user?.name || "관리자"} navItems={adminNavItems}>
                <div className="flex items-center justify-center h-64">
                    <Loader2 className="w-8 h-8 animate-spin text-primary" />
                </div>
            </DashboardLayout>
        );
    }

    return (
        <DashboardLayout
            role="admin"
            userName={user?.name || profile?.name || "관리자"}
            navItems={adminNavItems}
        >
            <div className="max-w-3xl mx-auto space-y-6">
                {/* 페이지 헤더 */}
                <div className="flex items-center justify-between">
                    <div>
                        <h1 className="text-2xl font-bold text-foreground">내 프로필</h1>
                        <p className="text-muted-foreground mt-1">개인정보를 확인하고 수정할 수 있습니다</p>
                    </div>
                    {!isEditing ? (
                        <Button onClick={() => setIsEditing(true)}>
                            <Edit2 className="w-4 h-4 mr-2" />
                            수정
                        </Button>
                    ) : (
                        <div className="flex gap-2">
                            <Button variant="outline" onClick={handleCancel} disabled={isSaving}>
                                <X className="w-4 h-4 mr-2" />
                                취소
                            </Button>
                            <Button onClick={handleSave} disabled={isSaving}>
                                {isSaving ? (
                                    <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                                ) : (
                                    <Save className="w-4 h-4 mr-2" />
                                )}
                                저장
                            </Button>
                        </div>
                    )}
                </div>

                {/* 프로필 카드 */}
                <Card className="shadow-card border-0">
                    <CardHeader className="pb-4">
                        <div className="flex items-center gap-4">
                            <Avatar className="w-20 h-20">
                                <AvatarFallback className="bg-destructive/10 text-destructive text-2xl font-bold">
                                    {profile?.name?.charAt(0)}
                                </AvatarFallback>
                            </Avatar>
                            <div>
                                <CardTitle className="text-xl">{profile?.name}</CardTitle>
                                <CardDescription className="flex items-center gap-2 mt-1">
                                    <Badge className="bg-destructive/10 text-destructive border-0">관리자</Badge>
                                    <span className="text-muted-foreground">
                                        가입일: {profile?.createdAt?.split('T')[0] || '-'}
                                    </span>
                                </CardDescription>
                            </div>
                        </div>
                    </CardHeader>

                    <Separator />

                    <CardContent className="pt-6 space-y-6">
                        {/* 기본 정보 */}
                        <div className="space-y-4">
                            <h3 className="text-sm font-semibold text-muted-foreground uppercase tracking-wide">
                                기본 정보
                            </h3>

                            <div className="grid gap-4 md:grid-cols-2">
                                <div className="space-y-2">
                                    <Label htmlFor="name" className="flex items-center gap-2">
                                        <User className="w-4 h-4" />
                                        이름
                                    </Label>
                                    {isEditing ? (
                                        <Input
                                            id="name"
                                            value={formData.name}
                                            onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                                        />
                                    ) : (
                                        <p className="text-foreground py-2">{profile?.name || "-"}</p>
                                    )}
                                </div>

                                <div className="space-y-2">
                                    <Label htmlFor="phone" className="flex items-center gap-2">
                                        <Phone className="w-4 h-4" />
                                        연락처
                                    </Label>
                                    {isEditing ? (
                                        <Input
                                            id="phone"
                                            value={formData.phone}
                                            onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                                            placeholder="010-0000-0000"
                                        />
                                    ) : (
                                        <p className="text-foreground py-2">{profile?.phone || "-"}</p>
                                    )}
                                </div>
                            </div>

                            <div className="space-y-2">
                                <Label htmlFor="email" className="flex items-center gap-2">
                                    <Mail className="w-4 h-4" />
                                    이메일
                                </Label>
                                <p className="text-foreground py-2 text-muted-foreground">
                                    {profile?.email || adminInfo?.email || "-"}
                                    <span className="text-xs ml-2">(수정 불가)</span>
                                </p>
                            </div>
                        </div>

                        {/* 관리 권한 정보 */}
                        <div className="space-y-4">
                            <h3 className="text-sm font-semibold text-muted-foreground uppercase tracking-wide">
                                관리 권한
                            </h3>

                            <div className="grid gap-4 md:grid-cols-2">
                                <div className="flex items-center gap-3 p-3 bg-muted/50 rounded-lg">
                                    <Shield className="w-5 h-5 text-muted-foreground" />
                                    <div>
                                        <p className="text-sm text-muted-foreground">관리 레벨</p>
                                        <p className="font-medium">{getLevelName(adminInfo?.level)}</p>
                                    </div>
                                </div>

                                <div className="flex items-center gap-3 p-3 bg-muted/50 rounded-lg">
                                    <Building className="w-5 h-5 text-muted-foreground" />
                                    <div>
                                        <p className="text-sm text-muted-foreground">행정구역 코드</p>
                                        <p className="font-medium">{adminInfo?.admCode || "-"}</p>
                                    </div>
                                </div>
                            </div>

                            <div className="flex items-center gap-3 p-3 bg-muted/50 rounded-lg">
                                <MapPin className="w-5 h-5 text-muted-foreground" />
                                <div>
                                    <p className="text-sm text-muted-foreground">담당 지역</p>
                                    <p className="font-medium">{adminInfo?.admName || "-"}</p>
                                </div>
                            </div>
                        </div>

                        {/* 계정 보안 */}
                        <div className="space-y-4">
                            <h3 className="text-sm font-semibold text-muted-foreground uppercase tracking-wide">
                                계정 보안
                            </h3>
                            <div className="flex items-center justify-between p-4 bg-muted/50 rounded-lg">
                                <div>
                                    <p className="font-medium">비밀번호</p>
                                    <p className="text-sm text-muted-foreground">정기적으로 비밀번호를 변경해주세요</p>
                                </div>
                                <Button variant="outline" size="sm" disabled>
                                    비밀번호 변경
                                </Button>
                            </div>
                        </div>
                    </CardContent>
                </Card>
            </div>
        </DashboardLayout>
    );
};

export default AdminProfile;
