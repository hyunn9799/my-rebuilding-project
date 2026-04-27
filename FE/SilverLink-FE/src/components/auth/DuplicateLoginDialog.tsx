import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { AlertTriangle } from "lucide-react";

interface DuplicateLoginDialogProps {
  open: boolean;
  onConfirm: () => void;
  onCancel: () => void;
  isLoading?: boolean;
}

export const DuplicateLoginDialog = ({
  open,
  onConfirm,
  onCancel,
  isLoading = false
}: DuplicateLoginDialogProps) => {
  return (
    <AlertDialog open={open} onOpenChange={(open) => !open && onCancel()}>
      <AlertDialogContent>
        <AlertDialogHeader>
          <div className="flex items-center gap-3 mb-2">
            <div className="w-12 h-12 rounded-full bg-warning/10 flex items-center justify-center">
              <AlertTriangle className="w-6 h-6 text-warning" />
            </div>
            <AlertDialogTitle className="text-xl">다른 기기에서 로그인 중</AlertDialogTitle>
          </div>
          <AlertDialogDescription asChild>
            <div className="text-base space-y-3 pt-2">
              <p className="text-foreground font-medium">
                이미 다른 기기에서 로그인되어 있습니다.
              </p>
              <p className="text-muted-foreground">
                기존 로그인을 종료하고 이 기기에서 로그인하시겠습니까?
              </p>
              <div className="p-3 rounded-lg bg-muted/50 text-sm text-muted-foreground">
                <p className="font-medium text-foreground mb-1">💡 참고</p>
                <ul className="list-disc list-inside space-y-1">
                  <li>확인을 누르면 다른 기기에서 자동 로그아웃됩니다</li>
                  <li>취소하면 기존 로그인이 유지됩니다</li>
                </ul>
              </div>
            </div>
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel onClick={onCancel} disabled={isLoading}>
            취소
          </AlertDialogCancel>
          <AlertDialogAction onClick={onConfirm} disabled={isLoading}>
            {isLoading ? (
              <div className="flex items-center gap-2">
                <div className="w-4 h-4 border-2 border-primary-foreground/30 border-t-primary-foreground rounded-full animate-spin" />
                처리 중...
              </div>
            ) : (
              "확인 (기존 세션 종료)"
            )}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
};

//ㅇㅇㅇㅇㅇㅇ