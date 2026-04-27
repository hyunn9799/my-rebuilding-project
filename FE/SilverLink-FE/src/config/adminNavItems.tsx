import {
  Home, Users, UserPlus, UserCog, BarChart3, MessageSquare, Shield, Megaphone, Settings, FileText, MapPin, User, AlertOctagon, Database, Phone
} from "lucide-react";

export const adminNavItems = [
  { title: "홈", href: "/admin", icon: <Home className="w-5 h-5" /> },
  { title: "상담사 등록", href: "/admin/counselors/new", icon: <UserPlus className="w-5 h-5" /> },
  { title: "회원 관리", href: "/admin/members", icon: <Users className="w-5 h-5" /> },
  { title: "회원 등록", href: "/admin/register", icon: <UserPlus className="w-5 h-5" /> },
  { title: "배정 관리", href: "/admin/assignments", icon: <UserCog className="w-5 h-5" /> },
  { title: "복지 시설 관리", href: "/admin/facilities", icon: <MapPin className="w-5 h-5" /> },
  { title: "CallBot 테스트", href: "/admin/call-test", icon: <Phone className="w-5 h-5" /> },

  { title: "민감정보 요청", href: "/admin/sensitive-info", icon: <Shield className="w-5 h-5" /> },
  { title: "불편사항 관리", href: "/admin/complaints", icon: <MessageSquare className="w-5 h-5" /> },
  { title: "공지사항 관리", href: "/admin/notices", icon: <Megaphone className="w-5 h-5" /> },
  { title: "운영정책 관리", href: "/admin/policies", icon: <FileText className="w-5 h-5" /> },
  {
    title: "복지 서비스 관리",
    href: "/admin/welfare-services",
    icon: <Database className="h-5 w-5" />,
  },
  { title: "시스템 설정", href: "/admin/settings", icon: <Settings className="w-5 h-5" /> },
  { title: "내 프로필", href: "/my-profile", icon: <User className="w-5 h-5" /> },
];
