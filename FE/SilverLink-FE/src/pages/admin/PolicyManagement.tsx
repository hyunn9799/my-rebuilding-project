import { useState, useEffect } from "react";
import { useNavigate } from "react-router-dom";
import DashboardLayout from "@/components/layout/DashboardLayout";
import { adminNavItems } from "@/config/adminNavItems";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select";
import { Badge } from "@/components/ui/badge";
import { FileText, Plus, Pencil, Trash2, Eye, Search, Calendar, Loader2 } from "lucide-react";
import { toast } from "sonner";
import { useAuth } from "@/contexts/AuthContext";
import { useMutation, useQuery } from "@tanstack/react-query";
import { createPolicy, getAllPolicies, PolicyRequest, PolicyType, PolicyResponse } from "@/api/policies";

interface Policy {
  id: number;
  title: string;
  category: string;
  content: string;
  status: "ACTIVE" | "DRAFT" | "ARCHIVED";
  version: string;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
}

// Helper function to convert API response to local Policy format
const mapApiToPolicy = (apiPolicy: PolicyResponse): Policy => ({
  id: apiPolicy.id,
  title: apiPolicy.description || apiPolicy.policyName,
  category: apiPolicy.policyType,
  content: apiPolicy.content,
  status: apiPolicy.isMandatory ? "ACTIVE" : "DRAFT",
  version: apiPolicy.version,
  createdAt: apiPolicy.createdAt?.split('T')[0] || '',
  updatedAt: apiPolicy.updatedAt?.split('T')[0] || '',
  createdBy: "관리자",
});

const PolicyManagement = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [policies, setPolicies] = useState<Policy[]>([]);
  const [isCreating, setIsCreating] = useState(false);
  const [editingPolicy, setEditingPolicy] = useState<Policy | null>(null);
  const [searchTerm, setSearchTerm] = useState("");
  const [filterCategory, setFilterCategory] = useState<string>("ALL");
  const [filterStatus, setFilterStatus] = useState<string>("ALL");

  // Fetch policies from API
  const { data: apiPolicies, isLoading, refetch } = useQuery({
    queryKey: ['policies'],
    queryFn: getAllPolicies,
  });

  // Update local state when API data changes
  useEffect(() => {
    if (apiPolicies) {
      setPolicies(apiPolicies.map(mapApiToPolicy));
    }
  }, [apiPolicies]);


  const [formData, setFormData] = useState<{
    title: string;
    category: string;
    content: string;
    status: "ACTIVE" | "DRAFT" | "ARCHIVED";
    version: string;
  }>({
    title: "",
    category: "PRIVACY_POLICY",
    content: "",
    status: "DRAFT",
    version: "1.0",
  });

  const categories = [
    { value: "PRIVACY_POLICY", label: "개인정보 처리방침", policyType: "PRIVACY_POLICY" as PolicyType },
    { value: "TERMS_OF_SERVICE", label: "서비스 이용약관", policyType: "TERMS_OF_SERVICE" as PolicyType },
    { value: "SENSITIVE_INFO_CONSENT", label: "민감정보 처리 동의", policyType: "SENSITIVE_INFO_CONSENT" as PolicyType },
    { value: "THIRD_PARTY_PROVISION_CONSENT", label: "개인정보 제3자 제공 동의", policyType: "THIRD_PARTY_PROVISION_CONSENT" as PolicyType },
    { value: "LOCATION_BASED_SERVICE", label: "위치기반 서비스 이용약관", policyType: "LOCATION_BASED_SERVICE" as PolicyType },
    { value: "WELFARE_BENEFITS_NOTIFICATION", label: "복지 정보 및 혜택 수신 동의", policyType: "WELFARE_BENEFITS_NOTIFICATION" as PolicyType },
  ];

  const statusConfig = {
    ACTIVE: { label: "활성", color: "bg-success text-success-foreground" },
    DRAFT: { label: "임시저장", color: "bg-secondary text-secondary-foreground" },
    ARCHIVED: { label: "보관", color: "bg-muted text-muted-foreground" },
  };

  // API Mutation for creating policy
  const createPolicyMutation = useMutation({
    mutationFn: (request: PolicyRequest) => createPolicy(request),
    onSuccess: (response) => {
      // Add new policy to local state
      const newPolicy: Policy = {
        id: response.id,
        title: response.policyName,
        category: response.policyType,
        content: response.content,
        status: "ACTIVE",
        version: response.version,
        createdAt: response.createdAt,
        updatedAt: response.updatedAt,
        createdBy: user?.name || "관리자",
      };
      setPolicies([newPolicy, ...policies]);
      toast.success("정책이 생성되었습니다");
      setIsCreating(false);
      setEditingPolicy(null);
    },
    onError: (error: any) => {
      const errorMessage = error.response?.data?.message || error.message || "정책 생성에 실패했습니다";
      toast.error(errorMessage);
    },
  });

  const handleCreate = () => {
    setIsCreating(true);
    setEditingPolicy(null);
    setFormData({
      title: "",
      category: "PRIVACY_POLICY",
      content: "",
      status: "DRAFT",
      version: "1.0",
    });
  };

  const handleEdit = (policy: Policy) => {
    setEditingPolicy(policy);
    setIsCreating(true);
    setFormData({
      title: policy.title,
      category: policy.category,
      content: policy.content,
      status: policy.status,
      version: policy.version,
    });
  };

  const handleSave = () => {
    if (!formData.title || !formData.content) {
      toast.error("필수 항목을 입력해주세요");
      return;
    }

    if (editingPolicy) {
      // For now, editing still uses local state (backend doesn't have update endpoint yet)
      setPolicies(policies.map(p =>
        p.id === editingPolicy.id
          ? { ...p, ...formData, updatedAt: new Date().toISOString().split('T')[0] }
          : p
      ));
      toast.success("정책이 수정되었습니다");
      setIsCreating(false);
      setEditingPolicy(null);
    } else {
      // Create new policy via API
      const selectedCategory = categories.find(cat => cat.value === formData.category);
      if (!selectedCategory) {
        toast.error("유효한 카테고리를 선택해주세요");
        return;
      }

      const request: PolicyRequest = {
        policyType: selectedCategory.policyType,
        version: formData.version,
        content: formData.content,
        isMandatory: formData.status === "ACTIVE", // ACTIVE means mandatory
        description: formData.title, // Use title as description
      };

      createPolicyMutation.mutate(request);
    }
  };

  const handleDelete = (id: number) => {
    if (confirm("정말 삭제하시겠습니까?")) {
      setPolicies(policies.filter(p => p.id !== id));
      toast.success("정책이 삭제되었습니다");
    }
  };

  const filteredPolicies = policies.filter(policy => {
    const matchesSearch = policy.title.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesCategory = filterCategory === "ALL" || policy.category === filterCategory;
    const matchesStatus = filterStatus === "ALL" || policy.status === filterStatus;
    return matchesSearch && matchesCategory && matchesStatus;
  });

  return (
    <DashboardLayout
      role="admin"
      userName={user?.name || "관리자"}
      navItems={adminNavItems}
    >
      <div className="space-y-6">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-foreground">운영정책 관리</h1>
            <p className="text-muted-foreground mt-1">서비스 운영정책 및 약관을 관리합니다</p>
          </div>
          {!isCreating && (
            <Button onClick={handleCreate} className="gap-2 w-full sm:w-auto">
              <Plus className="w-4 h-4" />
              새 정책 작성
            </Button>
          )}
        </div>

        {isCreating ? (
          /* Create/Edit Form */
          <Card className="shadow-card border-0">
            <CardHeader>
              <CardTitle>{editingPolicy ? "정책 수정" : "새 정책 작성"}</CardTitle>
              <CardDescription>운영정책 정보를 입력하세요</CardDescription>
            </CardHeader>
            <CardContent className="space-y-6">
              <div className="grid md:grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="title">정책 제목 *</Label>
                  <Input
                    id="title"
                    value={formData.title}
                    onChange={(e) => setFormData({ ...formData, title: e.target.value })}
                    placeholder="예: 개인정보 처리방침"
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="category">카테고리 *</Label>
                  <Select
                    value={formData.category}
                    onValueChange={(value) => setFormData({ ...formData, category: value })}
                  >
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {categories.map(cat => (
                        <SelectItem key={cat.value} value={cat.value}>
                          {cat.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>

              <div className="grid md:grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="version">버전</Label>
                  <Input
                    id="version"
                    value={formData.version}
                    onChange={(e) => setFormData({ ...formData, version: e.target.value })}
                    placeholder="1.0"
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="status">상태</Label>
                  <Select
                    value={formData.status}
                    onValueChange={(value: any) => setFormData({ ...formData, status: value })}
                  >
                    <SelectTrigger>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="DRAFT">임시저장</SelectItem>
                      <SelectItem value="ACTIVE">활성</SelectItem>
                      <SelectItem value="ARCHIVED">보관</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </div>

              <div className="space-y-2">
                <Label htmlFor="content">정책 내용 *</Label>
                <Textarea
                  id="content"
                  value={formData.content}
                  onChange={(e) => setFormData({ ...formData, content: e.target.value })}
                  placeholder="정책 내용을 입력하세요..."
                  rows={15}
                  className="font-mono text-sm"
                />
              </div>

              <div className="flex flex-col sm:flex-row gap-3">
                <Button onClick={handleSave} className="flex-1">
                  {editingPolicy ? "수정 완료" : "저장"}
                </Button>
                <Button
                  variant="outline"
                  onClick={() => {
                    setIsCreating(false);
                    setEditingPolicy(null);
                  }}
                  className="flex-1"
                >
                  취소
                </Button>
              </div>
            </CardContent>
          </Card>
        ) : (
          <>
            {/* Filters */}
            <Card className="shadow-card border-0">
              <CardContent className="p-4">
                <div className="flex flex-col md:flex-row gap-4">
                  <div className="flex-1 relative">
                    <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-muted-foreground" />
                    <Input
                      placeholder="정책 제목 검색..."
                      value={searchTerm}
                      onChange={(e) => setSearchTerm(e.target.value)}
                      className="pl-9"
                    />
                  </div>
                  <Select value={filterCategory} onValueChange={setFilterCategory}>
                    <SelectTrigger className="w-full md:w-48">
                      <SelectValue placeholder="카테고리" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="ALL">전체 카테고리</SelectItem>
                      {categories.map(cat => (
                        <SelectItem key={cat.value} value={cat.value}>
                          {cat.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <Select value={filterStatus} onValueChange={setFilterStatus}>
                    <SelectTrigger className="w-full md:w-40">
                      <SelectValue placeholder="상태" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="ALL">전체 상태</SelectItem>
                      <SelectItem value="ACTIVE">활성</SelectItem>
                      <SelectItem value="DRAFT">임시저장</SelectItem>
                      <SelectItem value="ARCHIVED">보관</SelectItem>
                    </SelectContent>
                  </Select>
                </div>
              </CardContent>
            </Card>

            {/* Policies List */}
            <div className="space-y-4">
              {filteredPolicies.length === 0 ? (
                <Card className="shadow-card border-0">
                  <CardContent className="p-12 text-center">
                    <FileText className="w-12 h-12 text-muted-foreground mx-auto mb-4" />
                    <p className="text-muted-foreground">등록된 정책이 없습니다</p>
                  </CardContent>
                </Card>
              ) : (
                filteredPolicies.map((policy) => (
                  <Card key={policy.id} className="shadow-card border-0 hover:shadow-elevated transition-shadow">
                    <CardContent className="p-4 sm:p-6">
                      <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4">
                        <div className="flex-1 space-y-3">
                          <div className="flex flex-wrap items-center gap-2">
                            <h3 className="text-base sm:text-lg font-semibold text-foreground">{policy.title}</h3>
                            <Badge className={statusConfig[policy.status].color}>
                              {statusConfig[policy.status].label}
                            </Badge>
                            <Badge variant="outline">v{policy.version}</Badge>
                          </div>
                          <p className="text-sm text-muted-foreground line-clamp-2">
                            {policy.content}
                          </p>
                          <div className="flex flex-col sm:flex-row sm:items-center gap-2 sm:gap-4 text-xs text-muted-foreground">
                            <span className="flex items-center gap-1">
                              <Calendar className="w-3 h-3" />
                              생성: {policy.createdAt}
                            </span>
                            <span>수정: {policy.updatedAt}</span>
                            <span>작성자: {policy.createdBy}</span>
                          </div>
                        </div>
                        <div className="flex gap-2 self-end sm:self-start">
                          <Button variant="ghost" size="icon" onClick={() => handleEdit(policy)}>
                            <Pencil className="w-4 h-4 text-blue-500" />
                          </Button>
                          <Button
                            variant="ghost"
                            size="icon"
                            onClick={() => handleDelete(policy.id)}
                            className="text-destructive hover:text-destructive"
                          >
                            <Trash2 className="w-4 h-4" />
                          </Button>
                        </div>
                      </div>
                    </CardContent>
                  </Card>
                ))
              )}
            </div>
          </>
        )}
      </div>
    </DashboardLayout>
  );
};

export default PolicyManagement;
