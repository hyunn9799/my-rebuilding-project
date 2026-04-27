import { useState, useEffect } from "react";
import { User, Phone, Mail, Building, BadgeCheck, Edit2, Save, X, Loader2, MapPin } from "lucide-react";
import DashboardLayout from "@/components/layout/DashboardLayout";
import { counselorNavItems } from "@/config/counselorNavItems";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import { useToast } from "@/hooks/use-toast";
import counselorsApi from "@/api/counselors";
import usersApi from "@/api/users";
import { CounselorResponse } from "@/types/api";
import { useAuth } from "@/contexts/AuthContext";

const CounselorProfile = () => {
    const { user } = useAuth();
    const { toast } = useToast();
    const [isLoading, setIsLoading] = useState(true);
    const [isEditing, setIsEditing] = useState(false);
    const [isSaving, setIsSaving] = useState(false);
    const [profile, setProfile] = useState<CounselorResponse | null>(null);

    // 수정용 폼 상태
    const [formData, setFormData] = useState({
        name: "",
        phone: "",
        email: "",
    });

    useEffect(() => {
        fetchProfile();
    }, []);

    const fetchProfile = async () => {
        try {
            setIsLoading(true);
            const data = await counselorsApi.getMyInfo();
            setProfile(data);
            setFormData({
                name: data.name || "",
                phone: data.phone || "",
                email: data.email || "",
            });
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
            email: profile?.email || "",
        });
        setIsEditing(false);
    };

    if (isLoading) {
        return (
            <DashboardLayout role="counselor" userName="로딩중..." navItems={counselorNavItems}>
                <div className="flex items-center justify-center h-64">
                    <Loader2 className="w-8 h-8 animate-spin text-primary" />
                </div>
            </DashboardLayout>
        );
    }

    return (
        <DashboardLayout
            role="counselor"
            userName={user?.name || "상담사"}
            navItems={counselorNavItems}
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
                                <AvatarFallback className="bg-primary/10 text-primary text-2xl font-bold">
                                    {profile?.name?.charAt(0)}
                                </AvatarFallback>
                            </Avatar>
                            <div>
                                <CardTitle className="text-xl">{profile?.name}</CardTitle>
                                <CardDescription className="flex items-center gap-2 mt-1">
                                    <Badge className="bg-primary/10 text-primary border-0">상담사</Badge>
                                    <span className="text-muted-foreground">
                                        담당 어르신: {profile?.assignedElderlyCount || 0}명
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
                                    {profile?.email || "-"}
                                    <span className="text-xs ml-2">(수정 불가)</span>
                                </p>
                            </div>
                        </div>

                        {/* 근무 정보 */}
                        <div className="space-y-4">
                            <h3 className="text-sm font-semibold text-muted-foreground uppercase tracking-wide">
                                근무 정보
                            </h3>

                            <div className="grid gap-4 md:grid-cols-2">
                                <div className="flex items-center gap-3 p-3 bg-muted/50 rounded-lg">
                                    <Building className="w-5 h-5 text-muted-foreground" />
                                    <div>
                                        <p className="text-sm text-muted-foreground">소속</p>
                                        <p className="font-medium">{profile?.department || "-"}</p>
                                    </div>
                                </div>

                                <div className="flex items-center gap-3 p-3 bg-muted/50 rounded-lg">
                                    <BadgeCheck className="w-5 h-5 text-muted-foreground" />
                                    <div>
                                        <p className="text-sm text-muted-foreground">사원번호</p>
                                        <p className="font-medium">{profile?.employeeNo || "-"}</p>
                                    </div>
                                </div>
                            </div>

                            <div className="flex items-center gap-3 p-3 bg-muted/50 rounded-lg">
                                <MapPin className="w-5 h-5 text-muted-foreground" />
                                <div>
                                    <p className="text-sm text-muted-foreground">담당 지역</p>
                                    <p className="font-medium">{profile?.fullAddress || "-"}</p>
                                </div>
                            </div>
                        </div>

                        {/* 계정 정보 */}
                        <div className="space-y-4">
                            <h3 className="text-sm font-semibold text-muted-foreground uppercase tracking-wide">
                                계정 정보
                            </h3>
                            <div className="flex items-center justify-between p-4 bg-muted/50 rounded-lg">
                                <div>
                                    <p className="font-medium">비밀번호</p>
                                    <p className="text-sm text-muted-foreground">비밀번호 변경은 관리자에게 문의하세요</p>
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

export default CounselorProfile;
