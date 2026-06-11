package com.ski.inventory.monitoring;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Po každém požadavku, který skončil 5xx, zapíše záznam do {@link ServerErrorRecorder}.
 */
@Component
public class ServerErrorRecordingFilter extends OncePerRequestFilter {

    private final ServerErrorRecorder recorder;

    public ServerErrorRecordingFilter(ServerErrorRecorder recorder) {
        this.recorder = recorder;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        StatusCapturingResponseWrapper wrapper = new StatusCapturingResponseWrapper(response);
        filterChain.doFilter(request, wrapper);
        int status = wrapper.getCapturedStatus();
        if (status >= 500 && status < 600) {
            recorder.record(request.getMethod(), request.getRequestURI(), status);
        }
    }

    private static class StatusCapturingResponseWrapper extends HttpServletResponseWrapper {
        private int status = 200;

        public StatusCapturingResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void setStatus(int sc) {
            this.status = sc;
            super.setStatus(sc);
        }

        @Override
        public void sendError(int sc) throws IOException {
            this.status = sc;
            super.sendError(sc);
        }

        @Override
        public void sendError(int sc, String msg) throws IOException {
            this.status = sc;
            super.sendError(sc, msg);
        }

        int getCapturedStatus() {
            return status;
        }
    }
}
