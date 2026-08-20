package com.project.common.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component("httpRequestMetricsFilter")
public class HttpRequestMetricsFilter extends OncePerRequestFilter {

    private static final String METRICS_PATH = "/actuator/prometheus";
    private static final String UNKNOWN_URI = "UNKNOWN";
    private static final String NO_EXCEPTION = "None";

    private final MeterRegistry meterRegistry;

    public HttpRequestMetricsFilter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return METRICS_PATH.equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Timer.Sample sample = Timer.start(meterRegistry);
        Exception requestException = null;
        try {
            filterChain.doFilter(request, response);
        } catch (ServletException | IOException | RuntimeException exception) {
            requestException = exception;
            throw exception;
        } finally {
            stopTimer(sample, request, response, requestException);
        }
    }

    private void stopTimer(
            Timer.Sample sample,
            HttpServletRequest request,
            HttpServletResponse response,
            Exception exception) {
        String uri = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (uri == null || uri.isBlank()) {
            uri = UNKNOWN_URI;
        }

        int statusCode = response.getStatus();
        String exceptionName = exception == null
                ? NO_EXCEPTION
                : exception.getClass().getSimpleName();
        Timer timer = Timer.builder("http.server.requests")
                .description("Duration of HTTP server request handling")
                .tag("method", request.getMethod())
                .tag("uri", uri)
                .tag("status", Integer.toString(statusCode))
                .tag("outcome", resolveOutcome(statusCode))
                .tag("exception", exceptionName)
                .publishPercentileHistogram()
                .register(meterRegistry);
        sample.stop(timer);
    }

    private String resolveOutcome(int statusCode) {
        HttpStatus status = HttpStatus.resolve(statusCode);
        return status == null ? "UNKNOWN" : status.series().name();
    }
}
