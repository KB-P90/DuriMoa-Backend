package com.project.common.monitoring;

import io.micrometer.prometheus.PrometheusMeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PrometheusMetricsController {

    private static final MediaType PROMETHEUS_MEDIA_TYPE =
            MediaType.parseMediaType("text/plain;version=0.0.4;charset=utf-8");

    private final PrometheusMeterRegistry prometheusMeterRegistry;

    @GetMapping(value = "/actuator/prometheus", produces = "text/plain")
    public ResponseEntity<String> scrape() {
        return ResponseEntity.ok()
                .contentType(PROMETHEUS_MEDIA_TYPE)
                .body(prometheusMeterRegistry.scrape());
    }
}
