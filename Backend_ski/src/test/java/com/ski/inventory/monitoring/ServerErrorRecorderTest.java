package com.ski.inventory.monitoring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ServerErrorRecorderTest {

    private ServerErrorRecorder recorder;

    @BeforeEach
    void setUp() {
        recorder = new ServerErrorRecorder();
    }

    @Test
    void getRecent_emptyInitially() {
        assertThat(recorder.getRecent()).isEmpty();
    }

    @Test
    void record_singleError_canBeRetrieved() {
        recorder.record("GET", "/api/test", 500);

        List<ServerErrorRecorder.RecordedError> errors = recorder.getRecent();
        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).method()).isEqualTo("GET");
        assertThat(errors.get(0).path()).isEqualTo("/api/test");
        assertThat(errors.get(0).status()).isEqualTo(500);
        assertThat(errors.get(0).timestamp()).isPositive();
    }

    @Test
    void record_multipleErrors_returnedInReverseOrder() {
        recorder.record("GET", "/path1", 500);
        recorder.record("POST", "/path2", 503);
        recorder.record("PUT", "/path3", 502);

        List<ServerErrorRecorder.RecordedError> errors = recorder.getRecent();
        assertThat(errors).hasSize(3);
        // Most recent first
        assertThat(errors.get(0).path()).isEqualTo("/path3");
        assertThat(errors.get(1).path()).isEqualTo("/path2");
        assertThat(errors.get(2).path()).isEqualTo("/path1");
    }

    @Test
    void record_exceedsMaxRecent_oldestDropped() {
        for (int i = 0; i < 25; i++) {
            recorder.record("GET", "/path/" + i, 500);
        }

        List<ServerErrorRecorder.RecordedError> errors = recorder.getRecent();
        assertThat(errors).hasSize(20);
        // Most recent should be /path/24 (first in reversed list)
        assertThat(errors.get(0).path()).isEqualTo("/path/24");
    }

    @Test
    void record_differentHttpMethods_allStored() {
        recorder.record("GET", "/api", 500);
        recorder.record("POST", "/api", 503);
        recorder.record("DELETE", "/api", 502);

        List<ServerErrorRecorder.RecordedError> errors = recorder.getRecent();
        assertThat(errors).hasSize(3);
        assertThat(errors).extracting(ServerErrorRecorder.RecordedError::method)
                .containsExactlyInAnyOrder("GET", "POST", "DELETE");
    }

    @Test
    void getRecent_returnsDefensiveCopy() {
        recorder.record("GET", "/api", 500);
        List<ServerErrorRecorder.RecordedError> first = recorder.getRecent();
        List<ServerErrorRecorder.RecordedError> second = recorder.getRecent();

        assertThat(first).isNotSameAs(second);
        assertThat(first).isEqualTo(second);
    }

    @Test
    void recordedError_timestampIsCurrentTime() {
        long before = System.currentTimeMillis();
        recorder.record("GET", "/api", 500);
        long after = System.currentTimeMillis();

        long ts = recorder.getRecent().get(0).timestamp();
        assertThat(ts).isBetween(before, after);
    }
}
