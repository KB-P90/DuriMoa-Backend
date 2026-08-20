package com.project.common.monitoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletResponse;

class HttpRequestMetricsFilterTest {

    @Test
    void recordsRequestUsingControllerPatternInsteadOfRawUri() throws Exception {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        HttpRequestMetricsFilter filter = new HttpRequestMetricsFilter(registry);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/goal/123");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = (servletRequest, servletResponse) -> {
            servletRequest.setAttribute(
                    HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE,
                    "/api/goal/{id}");
            ((HttpServletResponse) servletResponse).setStatus(200);
        };

        filter.doFilter(request, response, filterChain);

        Timer timer = registry.find("http.server.requests")
                .tags(
                        "method", "GET",
                        "uri", "/api/goal/{id}",
                        "status", "200",
                        "outcome", "SUCCESSFUL",
                        "exception", "None")
                .timer();
        assertNotNull(timer);
        assertEquals(1L, timer.count());
    }
}
