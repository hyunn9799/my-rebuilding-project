-- Sliding Window Rate Limiter
-- 지정된 시간 창 내에서 요청 횟수를 제한합니다.
--
-- KEYS[1] = rate limit key (예: "rl:api:userId:42")
-- ARGV[1] = window size (초)
-- ARGV[2] = max requests
-- ARGV[3] = current timestamp (밀리초)
-- ARGV[4] = unique request id (UUID)
--
-- 반환: 1 = 허용, 0 = 초과 (차단)

local key = KEYS[1]
local windowSizeMs = tonumber(ARGV[1]) * 1000
local maxRequests = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local requestId = ARGV[4]

-- 1. 만료된 항목 제거 (현재 시간 - 창 크기 이전)
local windowStart = now - windowSizeMs
redis.call('ZREMRANGEBYSCORE', key, '-inf', windowStart)

-- 2. 현재 창 내 요청 수 확인
local currentCount = redis.call('ZCARD', key)

-- 3. 허용 여부 판단
if currentCount < maxRequests then
    -- 허용: 요청 기록 추가
    redis.call('ZADD', key, now, requestId)
    redis.call('PEXPIRE', key, windowSizeMs)
    return 1
else
    -- 초과: 차단
    return 0
end
