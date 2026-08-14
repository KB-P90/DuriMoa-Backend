package com.project.domain.pushdevice.domain;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushDeviceVO {

    private Long id;
    private Long userId;
    private String token;
    private String platform;
    private Boolean pushEnabled;
    private LocalDateTime createdAt;
}
