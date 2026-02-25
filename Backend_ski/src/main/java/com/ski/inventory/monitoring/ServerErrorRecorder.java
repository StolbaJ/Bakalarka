package com.ski.inventory.monitoring;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Drží poslední 5xx odpovědi pro zobrazení v admin monitoringu.
 */
@Component
public class ServerErrorRecorder {

    private static final int MAX_RECENT = 20;

    private final List<RecordedError> recent = new CopyOnWriteArrayList<>();

    public void record(String method, String path, int status) {
        RecordedError e = new RecordedError(Instant.now().toEpochMilli(), method, path, status);
        recent.add(e);
        while (recent.size() > MAX_RECENT) {
            recent.remove(0);
        }
    }

    public List<RecordedError> getRecent() {
        List<RecordedError> copy = new ArrayList<>(recent);
        Collections.reverse(copy);
        return copy;
    }

    public record RecordedError(long timestamp, String method, String path, int status) {}
}
