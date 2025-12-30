-- KEYS[1] = tokens key
-- KEYS[2] = timestamp key
-- ARGV[1] = capacity
-- ARGV[2] = refill_rate_per_second
-- ARGV[3] = current_time_millis

local capacity = tonumber(ARGV[1])
local refillRate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])

local tokens = tonumber(redis.call("GET", KEYS[1]))
local lastRefill = tonumber(redis.call("GET", KEYS[2]))

if tokens == nil then
    tokens = capacity
end

if lastRefill == nil then
    lastRefill = now
end

local deltaMillis = now - lastRefill
local refillTokens = (deltaMillis / 1000) * refillRate
tokens = math.min(capacity, tokens + refillTokens)

local allowed = 0
if tokens >= 1 then
    tokens = tokens - 1
    allowed = 1
end

redis.call("SET", KEYS[1], tokens)
redis.call("SET", KEYS[2], now)

return { allowed, tokens }
