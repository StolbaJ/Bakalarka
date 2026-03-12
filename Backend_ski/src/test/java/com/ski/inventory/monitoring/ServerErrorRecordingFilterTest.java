package com.ski.inventory.monitoring;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServerErrorRecordingFilterTest {

    @Mock
    private ServerErrorRecorder recorder;

    private ServerErrorRecordingFilter filter;

    @BeforeEach
    void setUp() {
        filter = new ServerErrorRecordingFilter(recorder);
    }

    @Test
    void doFilterInternal_200Response_doesNotRecord() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/skis");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, (req, res) -> {
            ((HttpServletResponse) res).setStatus(200);
        });

        verify(recorder, never()).record(any(), any(), anyInt());
    }

    @Test
    void doFilterInternal_404Response_doesNotRecord() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/notfound");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, (req, res) -> {
            ((HttpServletResponse) res).setStatus(404);
        });

        verify(recorder, never()).record(any(), any(), anyInt());
    }

    @Test
    void doFilterInternal_500Response_records() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/crash");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, (req, res) -> {
            ((HttpServletResponse) res).setStatus(500);
        });

        verify(recorder).record("GET", "/api/crash", 500);
    }

    @Test
    void doFilterInternal_503Response_records() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/service");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, (req, res) -> {
            ((HttpServletResponse) res).setStatus(503);
        });

        verify(recorder).record("POST", "/api/service", 503);
    }

    @Test
    void doFilterInternal_599Response_records() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/resource");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, (req, res) -> {
            ((HttpServletResponse) res).setStatus(599);
        });

        verify(recorder).record("DELETE", "/api/resource", 599);
    }

    @Test
    void doFilterInternal_sendError500_records() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/update");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, (req, res) -> {
            try {
                ((HttpServletResponse) res).sendError(500);
            } catch (Exception ignored) {}
        });

        verify(recorder).record("PUT", "/api/update", 500);
    }

    @Test
    void doFilterInternal_sendErrorWithMessage_records() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("PATCH", "/api/patch");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, (req, res) -> {
            try {
                ((HttpServletResponse) res).sendError(502, "Bad Gateway");
            } catch (Exception ignored) {}
        });

        verify(recorder).record("PATCH", "/api/patch", 502);
    }

    @Test
    void doFilterInternal_defaultStatus200_doesNotRecord() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/ok");
        MockHttpServletResponse response = new MockHttpServletResponse();

        // No status set → default wrapper status is 200
        filter.doFilterInternal(request, response, (req, res) -> {
            // do nothing - status stays at default 200
        });

        verify(recorder, never()).record(any(), any(), anyInt());
    }
}
