package com.pandaterry.concurrent_entity_change_logger.entity_change_logging.domain.vo;

import lombok.Getter;
import lombok.Value;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Value
@Getter
public class SubmissionContext {
    private final String threadName;
    private final String hostname;
    private final Instant submittedAt;
    private final ContextData additionalData;

    private SubmissionContext(String threadName, String hostname,
                              Instant submittedAt, ContextData additionalData) {
        this.threadName = threadName;
        this.hostname = hostname;
        this.submittedAt = submittedAt;
        this.additionalData = additionalData;
    }

    public static SubmissionContext current() {
        return new SubmissionContext(
                Thread.currentThread().getName(),
                getHostname(),
                Instant.now(),
                ContextData.empty()
        );
    }

    public static SubmissionContext from(Map<String, Object> contextMap) {
        return new SubmissionContext(
                (String) contextMap.get("threadName"),
                (String) contextMap.get("hostname"),
                Instant.parse((String) contextMap.get("submittedAt")),
                ContextData.from(contextMap)
        );
    }

    public SubmissionContext enrichWith(ContextData additionalContext) {
        ContextData merged = this.additionalData.mergeWith(additionalContext);
        return new SubmissionContext(threadName, hostname, submittedAt, merged);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>(additionalData.getData());

        map.put("threadName", threadName);
        map.put("hostname", hostname);
        map.put("submittedAt", submittedAt.toString());
        return map;
    }

    private static String getHostname() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubmissionContext that)) return false;
        return Objects.equals(threadName, that.threadName) &&
                Objects.equals(hostname, that.hostname) &&
                Objects.equals(submittedAt, that.submittedAt) &&
                Objects.equals(additionalData, that.additionalData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(threadName, hostname, submittedAt, additionalData);
    }
}
