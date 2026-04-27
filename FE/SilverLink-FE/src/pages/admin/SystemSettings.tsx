import { useState } from "react";
import DashboardLayout from "@/components/layout/DashboardLayout";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";
import { Textarea } from "@/components/ui/textarea";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { toast } from "sonner";
import {
  Bell,
  Mail,
  MessageSquare,
  Phone,
  Save,
  RefreshCw,
  Globe
} from "lucide-react";
import { adminNavItems } from "@/config/adminNavItems";
import { useAuth } from "@/contexts/AuthContext";
import { useMaintenance } from "@/contexts/MaintenanceContext";

const SystemSettings = () => {
  const { user } = useAuth();
  const { isMaintenanceMode, setMaintenanceMode } = useMaintenance();
  const [isSaving, setIsSaving] = useState(false);

  // 일반 설정
  const [generalSettings, setGeneralSettings] = useState({
    siteName: "마음돌봄",
    siteDescription: "국가 복지 서비스 - AI 기반 어르신 안심 돌봄 시스템",
    contactEmail: "support@maumdolbom.go.kr",
    contactPhone: "1588-0000",
    operatingHours: "09:00 - 18:00",
    maintenanceMode: isMaintenanceMode,
  });

  // 알림 설정
  const [notificationSettings, setNotificationSettings] = useState({
    smsNotifications: true,
    pushNotifications: true,
    emergencyAlertSms: true,
    emergencyAlertPush: true,
  });

  const hours = Array.from({ length: 24 }, (_, i) => `${i.toString().padStart(2, '0')}:00`);

  const handleSave = async (section: string) => {
    setIsSaving(true);
    await new Promise((resolve) => setTimeout(resolve, 1000));

    if (section === "일반") {
      setMaintenanceMode(generalSettings.maintenanceMode);
    }

    toast.success(`${section} 설정이 저장되었습니다.`);
    setIsSaving(false);
  };

  const handleTimeChange = (type: 'start' | 'end', value: string) => {
    const [start, end] = generalSettings.operatingHours.split(' - ');
    const newTime = type === 'start' ? `${value} - ${end}` : `${start} - ${value}`;
    setGeneralSettings({ ...generalSettings, operatingHours: newTime });
  };

  return (
    <DashboardLayout
      role="admin"
      userName={user?.name || "관리자"}
      navItems={adminNavItems}
    >
      <div className="space-y-6">
        {/* Header */}
        <div>
          <h1 className="text-2xl font-bold text-foreground">시스템 설정</h1>
          <p className="text-muted-foreground">시스템 전반의 설정을 관리합니다</p>
        </div>

        <Tabs defaultValue="general" className="space-y-6">
          <TabsList className="grid w-full grid-cols-2">
            <TabsTrigger value="general" className="gap-2">
              <Globe className="w-4 h-4" />
              일반
            </TabsTrigger>
            <TabsTrigger value="notifications" className="gap-2">
              <Bell className="w-4 h-4" />
              알림
            </TabsTrigger>
          </TabsList>

          {/* 일반 설정 */}
          <TabsContent value="general">
            <Card>
              <CardHeader>
                <CardTitle>일반 설정</CardTitle>
                <CardDescription>사이트 기본 정보를 설정합니다</CardDescription>
              </CardHeader>
              <CardContent className="space-y-6">
                <div className="grid gap-6 md:grid-cols-2">
                  <div className="space-y-2">
                    <Label htmlFor="siteName">사이트 이름</Label>
                    <div className="p-2 font-medium text-black">
                      {generalSettings.siteName}
                    </div>
                  </div>
                  <div className="space-y-2">
                    <Label>운영 시간</Label>
                    <div className="flex items-center gap-2">
                      <Select
                        value={generalSettings.operatingHours.split(' - ')[0]}
                        onValueChange={(value) => handleTimeChange('start', value)}
                      >
                        <SelectTrigger className="w-[140px]">
                          <SelectValue placeholder="시작 시간" />
                        </SelectTrigger>
                        <SelectContent>
                          {hours.map((time) => (
                            <SelectItem key={`start-${time}`} value={time}>
                              {time}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                      <span className="text-muted-foreground">~</span>
                      <Select
                        value={generalSettings.operatingHours.split(' - ')[1]}
                        onValueChange={(value) => handleTimeChange('end', value)}
                      >
                        <SelectTrigger className="w-[140px]">
                          <SelectValue placeholder="종료 시간" />
                        </SelectTrigger>
                        <SelectContent>
                          {hours.map((time) => (
                            <SelectItem key={`end-${time}`} value={time}>
                              {time}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </div>
                  </div>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="siteDescription">사이트 설명</Label>
                  <Textarea
                    id="siteDescription"
                    value={generalSettings.siteDescription}
                    onChange={(e) =>
                      setGeneralSettings({ ...generalSettings, siteDescription: e.target.value })
                    }
                    rows={3}
                  />
                </div>
                <div className="grid gap-6 md:grid-cols-2">
                  <div className="space-y-2">
                    <Label htmlFor="contactEmail">문의 이메일</Label>
                    <div className="relative">
                      <Mail className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                      <Input
                        id="contactEmail"
                        type="email"
                        value={generalSettings.contactEmail}
                        onChange={(e) =>
                          setGeneralSettings({ ...generalSettings, contactEmail: e.target.value })
                        }
                        className="pl-9"
                      />
                    </div>
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="contactPhone">문의 전화</Label>
                    <div className="relative">
                      <Phone className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                      <Input
                        id="contactPhone"
                        value={generalSettings.contactPhone}
                        onChange={(e) =>
                          setGeneralSettings({ ...generalSettings, contactPhone: e.target.value })
                        }
                        className="pl-9"
                      />
                    </div>
                  </div>
                </div>
                <div className="flex items-center justify-between p-4 bg-warning/10 rounded-lg border border-warning/20">
                  <div className="space-y-1">
                    <p className="font-medium">유지보수 모드</p>
                    <p className="text-sm text-muted-foreground">
                      활성화 시 관리자 외 접근이 차단됩니다
                    </p>
                  </div>
                  <Switch
                    checked={generalSettings.maintenanceMode}
                    onCheckedChange={(checked) =>
                      setGeneralSettings({ ...generalSettings, maintenanceMode: checked })
                    }
                  />
                </div>
                <div className="flex justify-end">
                  <Button onClick={() => handleSave("일반")} disabled={isSaving}>
                    {isSaving ? (
                      <RefreshCw className="w-4 h-4 mr-2 animate-spin" />
                    ) : (
                      <Save className="w-4 h-4 mr-2" />
                    )}
                    저장
                  </Button>
                </div>
              </CardContent>
            </Card>
          </TabsContent>

          {/* 알림 설정 */}
          <TabsContent value="notifications">
            <Card>
              <CardHeader>
                <CardTitle>알림 설정</CardTitle>
                <CardDescription>알림 수신 방법을 설정합니다</CardDescription>
              </CardHeader>
              <CardContent className="space-y-6">
                <div className="space-y-4">
                  <h3 className="font-medium">기본 알림</h3>
                  <div className="space-y-3">
                    <div className="flex items-center justify-between p-3 bg-muted/50 rounded-lg">
                      <div className="flex items-center gap-3">
                        <MessageSquare className="w-5 h-5 text-muted-foreground" />
                        <span>SMS 알림</span>
                      </div>
                      <Switch
                        checked={notificationSettings.smsNotifications}
                        onCheckedChange={(checked) =>
                          setNotificationSettings({ ...notificationSettings, smsNotifications: checked })
                        }
                      />
                    </div>
                    <div className="flex items-center justify-between p-3 bg-muted/50 rounded-lg">
                      <div className="flex items-center gap-3">
                        <Bell className="w-5 h-5 text-muted-foreground" />
                        <span>푸시 알림</span>
                      </div>
                      <Switch
                        checked={notificationSettings.pushNotifications}
                        onCheckedChange={(checked) =>
                          setNotificationSettings({ ...notificationSettings, pushNotifications: checked })
                        }
                      />
                    </div>
                  </div>
                </div>
                <div className="space-y-4">
                  <h3 className="font-medium">긴급 알림</h3>
                  <div className="space-y-3">
                    <div className="flex items-center justify-between p-3 bg-destructive/10 rounded-lg border border-destructive/20">
                      <div className="flex items-center gap-3">
                        <MessageSquare className="w-5 h-5 text-destructive" />
                        <span>긴급상황 SMS 알림</span>
                      </div>
                      <Switch
                        checked={notificationSettings.emergencyAlertSms}
                        onCheckedChange={(checked) =>
                          setNotificationSettings({ ...notificationSettings, emergencyAlertSms: checked })
                        }
                      />
                    </div>
                    <div className="flex items-center justify-between p-3 bg-destructive/10 rounded-lg border border-destructive/20">
                      <div className="flex items-center gap-3">
                        <Bell className="w-5 h-5 text-destructive" />
                        <span>긴급상황 푸시 알림</span>
                      </div>
                      <Switch
                        checked={notificationSettings.emergencyAlertPush}
                        onCheckedChange={(checked) =>
                          setNotificationSettings({ ...notificationSettings, emergencyAlertPush: checked })
                        }
                      />
                    </div>
                  </div>
                </div>

                <div className="flex justify-end">
                  <Button onClick={() => handleSave("알림")} disabled={isSaving}>
                    {isSaving ? (
                      <RefreshCw className="w-4 h-4 mr-2 animate-spin" />
                    ) : (
                      <Save className="w-4 h-4 mr-2" />
                    )}
                    저장
                  </Button>
                </div>
              </CardContent>
            </Card>
          </TabsContent>
        </Tabs>
      </div>
    </DashboardLayout>
  );
};

export default SystemSettings;
