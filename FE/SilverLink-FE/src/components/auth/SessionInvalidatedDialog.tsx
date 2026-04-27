import {
    AlertDialog,
    AlertDialogAction,
    AlertDialogContent,
    AlertDialogDescription,
    AlertDialogFooter,
    AlertDialogHeader,
    AlertDialogTitle,
} from "@/components/ui/alert-dialog";
import { LogOut } from "lucide-react";

interface SessionInvalidatedDialogProps {
    open: boolean;
    onConfirm: () => void;
}

export const SessionInvalidatedDialog = ({
    open,
    onConfirm
}: SessionInvalidatedDialogProps) => {
    return (
        <AlertDialog open={open}>
            <AlertDialogContent>
                <AlertDialogHeader>
                    <div className="flex items-center gap-3 mb-2">
                        <div className="w-12 h-12 rounded-full bg-destructive/10 flex items-center justify-center">
                            <LogOut className="w-6 h-6 text-destructive" />
                        </div>
                        <AlertDialogTitle className="text-xl">다른 기기에서 로그인됨</AlertDialogTitle>
                    </div>
                    <AlertDialogDescription className="text-base space-y-3 pt-2">
                        <p className="text-foreground font-medium">
                            다른 기기에서 로그인하여 현재 세션이 종료되었습니다.
                        </p>
                        <p className="text-muted-foreground">
                            계속 사용하시려면 다시 로그인해주세요.
                        </p>
                        <div className="p-3 rounded-lg bg-muted/50 text-sm text-muted-foreground">
                            <p className="font-medium text-foreground mb-1">💡 안내</p>
                            <ul className="list-disc list-inside space-y-1">
                                <li>본인이 아닌 경우, 비밀번호를 변경해주세요</li>
                                <li>동일 계정은 한 기기에서만 로그인할 수 있습니다</li>
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
