package com.project.domain.notification.sse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Log4j2
@Component
public class NotificationEmitterRegistry {

    private static final Long EMITTER_TIMEOUT = 30 * 60 * 1000L;
    private static final long HEARTBEAT_INTERVAL_MILLIS = 20 * 1000L;

    private final Map<Long, List<SseEmitter>> emittersByUserId = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT);
        List<SseEmitter> userEmitters =
                emittersByUserId.computeIfAbsent(userId, id -> new CopyOnWriteArrayList<>());
        userEmitters.add(emitter);

        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(e -> remove(userId, emitter));

        send(userId, emitter, "connect", "connected");
        return emitter;
    }

    public void sendToUser(Long userId, String eventName, Object data) {
        List<SseEmitter> userEmitters = emittersByUserId.get(userId);
        if (userEmitters == null) {
            return;
        }
        for (SseEmitter emitter : userEmitters) {
            send(userId, emitter, eventName, data);
        }
    }

    @Scheduled(fixedRate = HEARTBEAT_INTERVAL_MILLIS)
    public void sendHeartbeat() {
        emittersByUserId.forEach((userId, userEmitters) -> {
            for (SseEmitter emitter : new ArrayList<>(userEmitters)) {
                try {
                    emitter.send(SseEmitter.event().comment("heartbeat"));
                } catch (IOException e) {
                    remove(userId, emitter);
                }
            }
        });
    }

    private void send(Long userId, SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data, MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            log.debug("SSE 전송에 실패하여 연결을 제거합니다. error={}", e.getMessage());
            remove(userId, emitter);
        }
    }

    private void remove(Long userId, SseEmitter emitter) {
        List<SseEmitter> userEmitters = emittersByUserId.get(userId);
        if (userEmitters == null) {
            return;
        }
        userEmitters.remove(emitter);
        if (userEmitters.isEmpty()) {
            emittersByUserId.remove(userId);
        }
    }
}
