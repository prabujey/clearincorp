package com.trivine.llc.api.service;

import com.trivine.llc.api.constants.ServiceConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j
public class RateLimiterService {

    private final ConcurrentHashMap<String, RequestData> requestCounts = new ConcurrentHashMap<>();

    private final int maxRequests = ServiceConstants.RATE_LIMIT_MAX_REQUESTS;
    private final long timeWindow = ServiceConstants.IP_RATE_LIMIT_TIME_WINDOW_MS;

    public boolean allowRequest(String key) {
        long currentTime = System.currentTimeMillis();

        RequestData data = requestCounts.compute(key, (k, existing) -> {
            if (existing == null || currentTime - existing.getStartTime() > timeWindow) {
                return new RequestData(currentTime);
            }
            existing.incrementCount();
            log.debug("Incrementing count for: {} | Count: {}", key, existing.getCount());
            return existing;
        });

        return data.getCount() <= maxRequests;
    }

    @Scheduled(fixedRateString = "${rate.limit.cleanup.ms:60000}")
    public void cleanup() {
        long now = System.currentTimeMillis();
        int removed = 0;
        var iterator = requestCounts.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (now - entry.getValue().getStartTime() > timeWindow) {
                iterator.remove();
                removed++;
            }
        }
        if (removed > 0) {
            log.debug("Cleaned up {} rate limit entries", removed);
        }
    }

    /**
     * Thread-safe request data holder using AtomicInteger for count.
     */
    private static class RequestData {
        private final AtomicInteger count;
        private final long startTime;

        public RequestData(long startTime) {
            this.count = new AtomicInteger(1);
            this.startTime = startTime;
        }

        public int getCount() {
            return count.get();
        }

        public void incrementCount() {
            count.incrementAndGet();
        }

        public long getStartTime() {
            return startTime;
        }
    }
}
