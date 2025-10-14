package me.fengorz.kiwi.tools.service;

import me.fengorz.kiwi.tools.config.ToolsProperties;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class RateLimitService {
    private final ToolsProperties props;
    private final Map<String, WindowCounter> writeMap = new ConcurrentHashMap<>();
    private final Map<String, WindowCounter> uploadMap = new ConcurrentHashMap<>();

    public RateLimitService(ToolsProperties props) { this.props = props; }

    public boolean allowWrite(String ip) {
        int v = increment(writeMap, ip);
        return v <= props.getWriteRateLimitPerMin();
    }

    public boolean allowUpload(String ip) {
        int v = increment(uploadMap, ip);
        return v <= props.getUploadRateLimitPerMin();
    }

    private int increment(Map<String, WindowCounter> map, String key) {
        long minute = Instant.now().getEpochSecond() / 60;
        WindowCounter counter = map.computeIfAbsent(key, k -> new WindowCounter(minute));
        return counter.increment(minute);
    }

    private static class WindowCounter {
        private volatile long windowMinute;
        private final AtomicInteger count = new AtomicInteger(0);
        WindowCounter(long m) { this.windowMinute = m; }
        int increment(long currentMinute) {
            if (currentMinute != windowMinute) {
                synchronized (this) {
                    if (currentMinute != windowMinute) {
                        windowMinute = currentMinute;
                        count.set(0);
                    }
                }
            }
            return count.incrementAndGet();
        }
    }
}
