package com.project.common.util;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Log4j2
@Component
public class FcmSender {

    // notification 필드를 실어 보내면 브라우저가 포그라운드 여부와 상관없이 자동으로
    // 시스템 알림을 띄워버려서, SSE 토스트와 중복으로 뜬다. data만 보내서 표시 여부/방식을
    // 전부 서비스워커(onBackgroundMessage) 쪽 코드가 직접 제어하도록 한다.
    public void send(String fcmToken, String title, String body, Map<String, String> data) {
        if (!StringUtils.hasText(fcmToken)) {
            return;
        }

        Map<String, String> payload = new HashMap<>(data != null ? data : Map.of());
        payload.put("title", title);
        payload.put("body", body);

        Message message = Message.builder()
                .setToken(fcmToken)
                .putAllData(payload)
                .build();

        try {
            FirebaseMessaging.getInstance().send(message);
        } catch (FirebaseMessagingException e) {
            log.warn("FCM 푸시 발송에 실패했습니다. token=[REDACTED], error={}", e.getMessage());
        }
    }
}
