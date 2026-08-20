package com.project.common.monitoring;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.micrometer.core.instrument.Counter;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class PrometheusMetricsControllerTest {

    @Test
    void returnsPrometheusTextFormat() {
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        Counter.builder("monitoring.test").register(registry).increment();
        PrometheusMetricsController controller = new PrometheusMetricsController(registry);

        ResponseEntity<String> response = controller.scrape();

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getHeaders().getContentType().toString().startsWith("text/plain"));
        assertTrue(response.getBody().contains("monitoring_test_total"));
    }
}
