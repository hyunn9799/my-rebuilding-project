import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { toast } from "sonner";
import {
  ArrowLeft,
  Heart,
  Thermometer,
  Droplets,
  Activity,
  Scale,
  Smile,
  Frown,
  Meh,
  CheckCircle
} from "lucide-react";

const SeniorHealth = () => {
  const navigate = useNavigate();
  const [step, setStep] = useState(1);
  const [healthData, setHealthData] = useState({
    bloodPressureHigh: "",
    bloodPressureLow: "",
    bloodSugar: "",
    temperature: "",
    weight: "",
    mood: "",
    notes: "",
  });
  const [isSubmitted, setIsSubmitted] = useState(false);

  const moods = [
    { id: "good", label: "좋아요", icon: <Smile className="w-12 h-12" />, color: "bg-success" },
    { id: "normal", label: "보통이에요", icon: <Meh className="w-12 h-12" />, color: "bg-warning" },
    { id: "bad", label: "안 좋아요", icon: <Frown className="w-12 h-12" />, color: "bg-destructive" },
  ];

  const handleSubmit = () => {
    setIsSubmitted(true);
    toast.success("오늘의 건강 기록이 저장되었습니다!");
  };

  if (isSubmitted) {
    return (
      <div className="min-h-screen bg-background flex flex-col">
        <header className="bg-success text-success-foreground p-6 rounded-b-3xl shadow-lg">
          <div className="flex items-center gap-4">
            <Button
              variant="ghost"
              size="lg"
              onClick={() => navigate("/senior")}
              className="text-success-foreground hover:bg-success-foreground/20 p-3"
            >
              <ArrowLeft className="w-8 h-8" />
            </Button>
            <div>
              <h1 className="text-2xl font-bold">건강 기록</h1>
              <p className="text-success-foreground/80 text-sm">오늘의 건강을 기록해요</p>
            </div>
          </div>
        </header>

        <main className="flex-1 flex items-center justify-center p-6">
          <Card className="w-full max-w-md">
            <CardContent className="p-8 text-center space-y-6">
              <div className="w-24 h-24 mx-auto rounded-full bg-success/10 flex items-center justify-center">
                <CheckCircle className="w-14 h-14 text-success" />
              </div>
              <div>
                <p className="text-2xl font-bold mb-2">저장 완료!</p>
                <p className="text-muted-foreground text-lg">
                  오늘의 건강 기록이<br />잘 저장되었어요.
                </p>
              </div>
              <Button
                onClick={() => navigate("/senior")}
                className="w-full h-16 text-xl font-bold rounded-2xl"
              >
                홈으로 돌아가기
              </Button>
            </CardContent>
          </Card>
        </main>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background pb-32">
      {/* Header */}
      <header className="bg-success text-success-foreground p-6 rounded-b-3xl shadow-lg">
        <div className="flex items-center gap-4">
          <Button
            variant="ghost"
            size="lg"
            onClick={() => navigate("/senior")}
            className="text-success-foreground hover:bg-success-foreground/20 p-3"
          >
            <ArrowLeft className="w-8 h-8" />
          </Button>
          <div>
            <h1 className="text-2xl font-bold">건강 기록</h1>
            <p className="text-success-foreground/80 text-sm">오늘의 건강을 기록해요</p>
          </div>
        </div>
      </header>

      {/* Progress */}
      <div className="px-6 py-4">
        <div className="flex gap-2">
          {[1, 2, 3].map((s) => (
            <div
              key={s}
              className={`h-2 flex-1 rounded-full ${
                s <= step ? "bg-success" : "bg-muted"
              }`}
            />
          ))}
        </div>
        <p className="text-center text-muted-foreground mt-2">
          {step} / 3 단계
        </p>
      </div>

      <main className="px-6 space-y-6">
        {/* Step 1: Vital Signs */}
        {step === 1 && (
          <div className="space-y-6">
            <Card>
              <CardContent className="p-6 space-y-6">
                <div className="flex items-center gap-3 mb-4">
                  <div className="w-12 h-12 rounded-xl bg-destructive/10 flex items-center justify-center">
                    <Heart className="w-7 h-7 text-destructive" />
                  </div>
                  <div>
                    <p className="font-bold text-lg">혈압</p>
                    <p className="text-muted-foreground text-sm">수축기/이완기</p>
                  </div>
                </div>
                <div className="flex gap-4">
                  <div className="flex-1">
                    <Label className="text-lg">수축기 (위)</Label>
                    <Input
                      type="number"
                      placeholder="120"
                      value={healthData.bloodPressureHigh}
                      onChange={(e) =>
                        setHealthData({ ...healthData, bloodPressureHigh: e.target.value })
                      }
                      className="h-16 text-2xl text-center mt-2"
                    />
                  </div>
                  <div className="flex-1">
                    <Label className="text-lg">이완기 (아래)</Label>
                    <Input
                      type="number"
                      placeholder="80"
                      value={healthData.bloodPressureLow}
                      onChange={(e) =>
                        setHealthData({ ...healthData, bloodPressureLow: e.target.value })
                      }
                      className="h-16 text-2xl text-center mt-2"
                    />
                  </div>
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardContent className="p-6 space-y-4">
                <div className="flex items-center gap-3">
                  <div className="w-12 h-12 rounded-xl bg-info/10 flex items-center justify-center">
                    <Droplets className="w-7 h-7 text-info" />
                  </div>
                  <div>
                    <p className="font-bold text-lg">혈당</p>
                    <p className="text-muted-foreground text-sm">mg/dL</p>
                  </div>
                </div>
                <Input
                  type="number"
                  placeholder="100"
                  value={healthData.bloodSugar}
                  onChange={(e) =>
                    setHealthData({ ...healthData, bloodSugar: e.target.value })
                  }
                  className="h-16 text-2xl text-center"
                />
              </CardContent>
            </Card>
          </div>
        )}

        {/* Step 2: Body Measurements */}
        {step === 2 && (
          <div className="space-y-6">
            <Card>
              <CardContent className="p-6 space-y-4">
                <div className="flex items-center gap-3">
                  <div className="w-12 h-12 rounded-xl bg-warning/10 flex items-center justify-center">
                    <Thermometer className="w-7 h-7 text-warning" />
                  </div>
                  <div>
                    <p className="font-bold text-lg">체온</p>
                    <p className="text-muted-foreground text-sm">°C</p>
                  </div>
                </div>
                <Input
                  type="number"
                  step="0.1"
                  placeholder="36.5"
                  value={healthData.temperature}
                  onChange={(e) =>
                    setHealthData({ ...healthData, temperature: e.target.value })
                  }
                  className="h-16 text-2xl text-center"
                />
              </CardContent>
            </Card>

            <Card>
              <CardContent className="p-6 space-y-4">
                <div className="flex items-center gap-3">
                  <div className="w-12 h-12 rounded-xl bg-primary/10 flex items-center justify-center">
                    <Scale className="w-7 h-7 text-primary" />
                  </div>
                  <div>
                    <p className="font-bold text-lg">체중</p>
                    <p className="text-muted-foreground text-sm">kg</p>
                  </div>
                </div>
                <Input
                  type="number"
                  step="0.1"
                  placeholder="60.0"
                  value={healthData.weight}
                  onChange={(e) =>
                    setHealthData({ ...healthData, weight: e.target.value })
                  }
                  className="h-16 text-2xl text-center"
                />
              </CardContent>
            </Card>
          </div>
        )}

        {/* Step 3: Mood and Notes */}
        {step === 3 && (
          <div className="space-y-6">
            <Card>
              <CardContent className="p-6">
                <p className="font-bold text-lg mb-4">오늘 기분이 어떠세요?</p>
                <div className="grid grid-cols-3 gap-4">
                  {moods.map((mood) => (
                    <button
                      key={mood.id}
                      onClick={() => setHealthData({ ...healthData, mood: mood.id })}
                      className={`p-6 rounded-2xl border-2 transition-all ${
                        healthData.mood === mood.id
                          ? `${mood.color} text-white border-transparent`
                          : "bg-card border-border hover:border-muted-foreground"
                      }`}
                    >
                      <div className="flex flex-col items-center gap-2">
                        {mood.icon}
                        <span className="font-bold">{mood.label}</span>
                      </div>
                    </button>
                  ))}
                </div>
              </CardContent>
            </Card>

            <Card>
              <CardContent className="p-6 space-y-4">
                <p className="font-bold text-lg">하고 싶은 말이 있으세요?</p>
                <Textarea
                  placeholder="오늘 특별히 느낀 점이나 아픈 곳이 있으면 적어주세요..."
                  value={healthData.notes}
                  onChange={(e) =>
                    setHealthData({ ...healthData, notes: e.target.value })
                  }
                  rows={4}
                  className="text-lg"
                />
              </CardContent>
            </Card>
          </div>
        )}
      </main>

      {/* Navigation Buttons */}
      <div className="fixed bottom-0 left-0 right-0 p-6 bg-gradient-to-t from-background via-background to-transparent">
        <div className="flex gap-4">
          {step > 1 && (
            <Button
              variant="outline"
              onClick={() => setStep(step - 1)}
              className="flex-1 h-16 text-lg font-bold rounded-2xl"
            >
              이전
            </Button>
          )}
          {step < 3 ? (
            <Button
              onClick={() => setStep(step + 1)}
              className="flex-1 h-16 text-lg font-bold rounded-2xl"
            >
              다음
            </Button>
          ) : (
            <Button
              onClick={handleSubmit}
              className="flex-1 h-16 text-lg font-bold rounded-2xl bg-success hover:bg-success/90"
            >
              저장하기
            </Button>
          )}
        </div>
      </div>
    </div>
  );
};

export default SeniorHealth;
