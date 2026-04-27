-- Atomic Session Swap: 기존 세션 무효화 + 새 세션 생성을 원자적으로 수행
-- KEYS[1] = user:{userId}:sid         (사용자→세션 매핑)
-- KEYS[2] = sess:{newSid}             (새 세션 데이터)
-- ARGV[1] = newSid                     (새 세션 ID)
-- ARGV[2] = idleSeconds               (TTL 초)
-- ARGV[3..n] = 세션 필드 키-값 쌍     (userId, role, refreshHash, lastSeen, ip, ua, deviceId, loginAt)
-- 반환: 기존 sid (있으면) 또는 빈 문자열

local userSidKey = KEYS[1]
local newSessKey = KEYS[2]
local newSid = ARGV[1]
local ttl = tonumber(ARGV[2])

-- 1. 기존 세션 확인
local oldSid = redis.call('GET', userSidKey)

-- 2. 기존 세션이 있으면 무효화
if oldSid and oldSid ~= '' then
    local oldSessKey = 'sess:' .. oldSid
    local invalidatedKey = 'sess:invalidated:' .. oldSid

    -- 강제 종료 마킹 (10분 유지)
    redis.call('SET', invalidatedKey, 'true', 'EX', 600)

    -- 기존 세션 삭제
    redis.call('DEL', oldSessKey)
end

-- 3. 새 세션 생성 (ARGV[3]부터 키-값 쌍)
local fieldCount = (#ARGV - 2)
if fieldCount > 0 then
    local fields = {}
    for i = 3, #ARGV do
        fields[#fields + 1] = ARGV[i]
    end
    redis.call('HSET', newSessKey, unpack(fields))
end

-- 4. TTL 설정
redis.call('EXPIRE', newSessKey, ttl)

-- 5. 사용자→세션 매핑 업데이트
redis.call('SET', userSidKey, newSid, 'EX', ttl)

return oldSid or ''
