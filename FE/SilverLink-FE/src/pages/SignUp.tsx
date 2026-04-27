import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import {
    Heart,
    Shield,
    Users,
    UserCog,
    Eye,
    EyeOff,
    UserPlus,
    ArrowLeft
} from "lucide-react";
import { toast } from "sonner";
import { signup } from "@/api/auth";
import { SignupRequest } from "@/types/api";

type UserRole = "guardian" | "counselor" | "admin" | "senior";

interface RoleConfig {
    id: UserRole;
    title: string;
    description: string;
    icon: React.ReactNode;
    color: string;
}

const roles: RoleConfig[] = [
    {
        id: "guardian",
        title: "보호자",
        description: "부모님의 안부와 상태를 확인하세요",
        icon: <Heart className="w-6 h-6" />,
        color: "bg-accent/10 text-accent border-accent/20",
    },
    {
        id: "counselor",
        title: "상담사",
        description: "담당 어르신을 관리하고 상담하세요",
        icon: <Users className="w-6 h-6" />,
        color: "bg-primary/10 text-primary border-primary/20",
    },
    {
        id: "admin",
        title: "관리자",
        description: "시스템 전체를 관리합니다",
        icon: <UserCog className="w-6 h-6" />,
        color: "bg-info/10 text-info border-info/20",
    },
    {
        id: "senior",
        title: "어르신",
        description: "방문등록 및 서비스 이용",
        icon: <Shield className="w-6 h-6" />,
        color: "bg-success/10 text-success border-success/20",
    },
];

const SignUp = () => {
    const navigate = useNavigate();
    const [selectedRole, setSelectedRole] = useState<UserRole>("guardian");
    const [showPassword, setShowPassword] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const [formData, setFormData] = useState<SignupRequest>({
        loginId: "",
        password: "",
        name: "",
        phone: "",
        email: "",
        role: "GUARDIAN", // Default role enum string
    });

    const handleRoleSelect = (roleId: UserRole) => {
        setSelectedRole(roleId);
        setFormData({ ...formData, role: roleId.toUpperCase() });
    };

    const handleSignup = async (e: React.FormEvent) => {
        e.preventDefault();
        setIsLoading(true);

        try {
            await signup(formData);

            toast.success("회원가입 성공", {
                description: "로그인 페이지로 이동합니다.",
            });
            navigate("/login");
        } catch (error: any) {
            const errorMessage = error.response?.data?.message || "회원가입에 실패했습니다.";
            toast.error("회원가입 실패", {
                description: errorMessage,
            });
        } finally {
            setIsLoading(false);
        }
    };

    return (
        <div className="min-h-screen bg-gradient-hero flex flex-col">
            {/* Header */}
            <header className="p-6">
                <div className="flex items-center gap-3 cursor-pointer" onClick={() => navigate("/")}>
                    <div className="w-10 h-10 rounded-xl bg-gradient-primary flex items-center justify-center">
                        <Heart className="w-5 h-5 text-primary-foreground" />
                    </div>
                    <div>
                        <h1 className="text-xl font-bold text-foreground">마음돌봄</h1>
                        <p className="text-xs text-muted-foreground">국가 복지 서비스</p>
                    </div>
                </div>
            </header>

            {/* Main Content */}
            <main className="flex-1 flex items-center justify-center p-6">
                <div className="w-full max-w-md space-y-6 animate-fade-in">

                    {/* Back Button */}
                    <button
                        onClick={() => navigate("/login")}
                        className="flex items-center gap-2 text-muted-foreground hover:text-foreground transition-colors mb-2"
                    >
                        <ArrowLeft className="w-4 h-4" />
                        로그인으로 돌아가기
                    </button>

                    {/* Title */}
                    <div className="text-center space-y-2">
                        <h2 className="text-2xl font-bold text-foreground">
                            회원가입
                        </h2>
                        <p className="text-muted-foreground">
                            서비스 이용을 위해 계정을 생성해주세요
                        </p>
                    </div>

                    {/* Role Selection */}
                    <div className="grid grid-cols-2 gap-3">
                        {roles.map((role) => (
                            <button
                                key={role.id}
                                type="button"
                                onClick={() => handleRoleSelect(role.id)}
                                className={`p-4 rounded-xl border-2 transition-all duration-200 text-left ${selectedRole === role.id
                                    ? `${role.color} border-current shadow-soft`
                                    : "bg-card border-border hover:border-muted-foreground/30"
                                    }`}
                            >
                                <div className="flex items-center gap-3">
                                    <div
                                        className={`w-10 h-10 rounded-lg flex items-center justify-center ${selectedRole === role.id ? role.color : "bg-muted"
                                            }`}
                                    >
                                        {role.icon}
                                    </div>
                                    <div>
                                        <p className="font-medium text-foreground">{role.title}</p>
                                        <p className="text-xs text-muted-foreground line-clamp-1">
                                            {role.description}
                                        </p>
                                    </div>
                                </div>
                            </button>
                        ))}
                    </div>

                    {/* Signup Form */}
                    <Card className="shadow-elevated border-0">
                        <CardHeader className="pb-4">
                            <CardTitle className="text-lg">
                                {roles.find((r) => r.id === selectedRole)?.title} 계정 생성
                            </CardTitle>
                            <CardDescription>
                                필수 정보를 입력해주세요
                            </CardDescription>
                        </CardHeader>
                        <CardContent>
                            <form onSubmit={handleSignup} className="space-y-4">
                                <div className="space-y-2">
                                    <Label htmlFor="loginId">아이디</Label>
                                    <Input
                                        id="loginId"
                                        placeholder="사용하실 아이디를 입력하세요"
                                        value={formData.loginId}
                                        onChange={(e) => setFormData({ ...formData, loginId: e.target.value })}
                                        required
                                    />
                                </div>

                                <div className="space-y-2">
                                    <Label htmlFor="password">비밀번호</Label>
                                    <div className="relative">
                                        <Input
                                            id="password"
                                            type={showPassword ? "text" : "password"}
                                            placeholder="비밀번호를 입력하세요"
                                            value={formData.password}
                                            onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                                            required
                                        />
                                        <button
                                            type="button"
                                            onClick={() => setShowPassword(!showPassword)}
                                            className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                                        >
                                            {showPassword ? (
                                                <EyeOff className="w-4 h-4" />
                                            ) : (
                                                <Eye className="w-4 h-4" />
                                            )}
                                        </button>
                                    </div>
                                </div>

                                <div className="space-y-2">
                                    <Label htmlFor="name">이름</Label>
                                    <Input
                                        id="name"
                                        placeholder="실명을 입력하세요"
                                        value={formData.name}
                                        onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                                        required
                                    />
                                </div>

                                <div className="space-y-2">
                                    <Label htmlFor="phone">휴대폰 번호</Label>
                                    <Input
                                        id="phone"
                                        type="tel"
                                        placeholder="01012345678"
                                        value={formData.phone}
                                        onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
                                        required
                                    />
                                </div>

                                <div className="space-y-2">
                                    <Label htmlFor="email">이메일 (선택)</Label>
                                    <Input
                                        id="email"
                                        type="email"
                                        placeholder="example@email.com"
                                        value={formData.email}
                                        onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                                    />
                                </div>

                                <Button
                                    type="submit"
                                    className="w-full mt-6"
                                    variant="hero"
                                    size="lg"
                                    disabled={isLoading}
                                >
                                    {isLoading ? (
                                        <div className="w-5 h-5 border-2 border-primary-foreground/30 border-t-primary-foreground rounded-full animate-spin" />
                                    ) : (
                                        <>
                                            <UserPlus className="w-4 h-4" />
                                            가입완료
                                        </>
                                    )}
                                </Button>
                            </form>
                        </CardContent>
                    </Card>
                </div>
            </main>
        </div>
    );
};

export default SignUp;
