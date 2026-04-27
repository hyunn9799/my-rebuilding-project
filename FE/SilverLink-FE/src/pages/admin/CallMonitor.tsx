import React, { useEffect, useState, useRef } from 'react';
import { useParams } from 'react-router-dom';
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Badge } from "@/components/ui/badge";

interface LogMessage {
    id: number;
    type: 'PROMPT' | 'REPLY';
    content: string;
    timestamp: Date;
}

const CallMonitor = () => {
    const { callId } = useParams();
    const [logs, setLogs] = useState<LogMessage[]>([]);
    const scrollRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        if (!callId) return;

        console.log(`🔌 [SSE] 연결 시도: callId=${callId}`);

        // 1. Fetch History
        fetch(`/api/internal/callbot/calls/${callId}/logs`)
            .then(res => {
                console.log(`📜 [SSE] 히스토리 로드 응답: status=${res.status}`);
                return res.json();
            })
            .then(data => {
                if (data.success) {
                    console.log(`✅ [SSE] 히스토리 로드 성공: ${data.data.length}개 메시지`);
                    const historyLogs: LogMessage[] = data.data.map((item: any) => ({
                        id: item.id, // Note: ID collision possible between PROMPT/REPLY if DB IDs overlap, better to postfix
                        type: item.type,
                        content: item.content,
                        timestamp: new Date(item.timestamp)
                    }));
                    setLogs(historyLogs);
                } else {
                    console.warn(`⚠️ [SSE] 히스토리 로드 실패:`, data);
                }
            })
            .catch(err => {
                console.error("❌ [SSE] 히스토리 로드 에러:", err);
            });

        // 2. Connect SSE
        const eventSource = new EventSource(`/api/internal/callbot/calls/${callId}/sse`);

        eventSource.onopen = () => {
            console.log(`✅ [SSE] 연결 성공: callId=${callId}, readyState=${eventSource.readyState}`);
        };

        eventSource.addEventListener('connect', () => {
            console.log(`🤝 [SSE] 서버 연결 확인됨: callId=${callId}`);
        });

        eventSource.addEventListener('prompt', (e: MessageEvent) => {
            console.log(`📤 [SSE] AI 발화 수신: callId=${callId}, data=${e.data.substring(0, 50)}...`);
            const newLog: LogMessage = {
                id: Date.now(),
                type: 'PROMPT',
                content: e.data,
                timestamp: new Date()
            };
            setLogs(prev => [...prev, newLog]);
        });

        eventSource.addEventListener('reply', (e: MessageEvent) => {
            console.log(`📥 [SSE] 어르신 응답 수신: callId=${callId}, data=${e.data.substring(0, 50)}...`);
            const newLog: LogMessage = {
                id: Date.now(),
                type: 'REPLY',
                content: e.data,
                timestamp: new Date()
            };
            setLogs(prev => [...prev, newLog]);
        });

        eventSource.onerror = (e) => {
            console.error(`❌ [SSE] 에러 발생: callId=${callId}, readyState=${eventSource.readyState}`, e);
            eventSource.close();
            console.log(`🔌 [SSE] 연결 종료됨: callId=${callId}`);
        };

        return () => {
            console.log(`🔌 [SSE] 컴포넌트 언마운트로 연결 종료: callId=${callId}`);
            eventSource.close();
        };
    }, [callId]);

    useEffect(() => {
        if (scrollRef.current) {
            scrollRef.current.scrollIntoView({ behavior: 'smooth' });
        }
    }, [logs]);

    return (
        <div className="p-6 max-w-4xl mx-auto">
            <Card className="h-[80vh] flex flex-col">
                <CardHeader>
                    <CardTitle className="flex items-center justify-between">
                        <span>실시간 통화 모니터링 (Call ID: {callId})</span>
                        <Badge variant="outline" className="animate-pulse text-green-600 border-green-600">
                            ● Live
                        </Badge>
                    </CardTitle>
                </CardHeader>
                <CardContent className="flex-1 overflow-hidden p-0">
                    <ScrollArea className="h-full p-4">
                        <div className="space-y-4">
                            {logs.map((log) => (
                                <div
                                    key={log.id}
                                    className={`flex ${log.type === 'PROMPT' ? 'justify-start' : 'justify-end'}`}
                                >
                                    <div
                                        className={`max-w-[80%] rounded-lg p-3 ${log.type === 'PROMPT'
                                            ? 'bg-secondary text-secondary-foreground'
                                            : 'bg-primary text-primary-foreground'
                                            }`}
                                    >
                                        <div className="text-xs opacity-70 mb-1">
                                            {log.type === 'PROMPT' ? 'CallBot' : '어르신'} • {log.timestamp.toLocaleTimeString()}
                                        </div>
                                        <div className="text-sm whitespace-pre-wrap">{log.content}</div>
                                    </div>
                                </div>
                            ))}
                            <div ref={scrollRef} />
                        </div>
                        {logs.length === 0 && (
                            <div className="flex h-full items-center justify-center text-muted-foreground">
                                대화 내용을 기다리고 있습니다...
                            </div>
                        )}
                    </ScrollArea>
                </CardContent>
            </Card>
        </div>
    );
};

export default CallMonitor;
