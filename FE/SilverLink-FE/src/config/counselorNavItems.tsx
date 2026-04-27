import {
  Home,
  Users,
  Phone,
  FileText,
  MessageSquare,
  Bell,
  Megaphone,
  Lock,
  User,
  Clock
} from "lucide-react";

export const counselorNavItems = [
  { title: "홈", href: "/counselor", icon: <Home className="w-5 h-5" /> },
  { title: "담당 어르신", href: "/counselor/seniors", icon: <Users className="w-5 h-5" /> },
  { title: "통화 기록", href: "/counselor/calls", icon: <Phone className="w-5 h-5" /> },
  { title: "상담 기록", href: "/counselor/records", icon: <FileText className="w-5 h-5" /> },
  { title: "스케줄 변경", href: "/counselor/schedule-requests", icon: <Clock className="w-5 h-5" /> },
  { title: "보호자 문의", href: "/counselor/inquiries", icon: <MessageSquare className="w-5 h-5" /> },
  { title: "긴급 알림", href: "/counselor/alerts", icon: <Bell className="w-5 h-5" /> },
  { title: "공지사항", href: "/counselor/notices", icon: <Megaphone className="w-5 h-5" /> },
  { title: "민감정보 요청", href: "/counselor/sensitive-info", icon: <Lock className="w-5 h-5" /> },
  { title: "내 프로필", href: "/my-profile", icon: <User className="w-5 h-5" /> },
];

