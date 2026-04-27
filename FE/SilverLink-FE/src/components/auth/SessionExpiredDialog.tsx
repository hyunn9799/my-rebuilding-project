import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { Clock } from "lucide-react";

interface SessionExpiredDialogProps {
    open: boolean;
    onConfirm: () => void;
}

export const SessionExpiredDialog = ({
    open,
    onConfirm
}: SessionExpiredDialogProps) => {
    return (
        <AlertDialog open={open}>
            <AlertDialogContent>
                <AlertDialogHeader>
                    <div className="flex items-center gap-3 mb-2">
                        <div className="w-12 h-12 rounded-full bg-orange-100 dark:bg-orange-900/30 flex items-center justify-center">
                            <Clock className="w-6 h-6 text-orange-500" />
                        </div>
                        <AlertDialogTitle className="text-xl">세션이 만료되었습니다</AlertDialogTitle>
                    </div>
                    <AlertDialogDescription className="text-base space-y-3 pt-2">
                        <p className="text-foreground font-medium">
                            장시간 활동이 없어 자동으로 로그아웃되었습니다.
                        </p>
                        <p className="text-muted-foreground">
                            보안을 위해 다시 로그인해주세요.
                        </p>
                        <div className="p-3 rounded-lg bg-muted/50 text-sm text-muted-foreground">
                            <p className="font-medium text-foreground mb-1">💡 안내</p>
                            <ul className="list-disc list-inside space-y-1">
                                <li>작업 중이던 내용이 저장되지 않았을 수 있습니다</li>
                                <li>로그인 후 다시 작업을 진행해주세요</li>
                            </ul>
                        </div>
                    </AlertDialogDescription>
                </AlertDialogHeader>
                <AlertDialogFooter>
                    <AlertDialogAction onClick={onConfirm} className="w-full sm:w-auto">
                        로그인 페이지로 이동
                    </AlertDialogAction>
                </AlertDialogFooter>
            </AlertDialogContent>
        </AlertDialog>
    );
};
