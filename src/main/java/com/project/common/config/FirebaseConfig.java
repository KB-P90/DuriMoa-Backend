package com.project.common.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    // GOOGLE_APPLICATION_CREDENTIALS 환경변수를 서버마다 따로 설정하지 않아도 되도록,
    // 서비스 계정 키를 classpath 리소스(src/main/resources/google-application.json,
    // codef.properties 등과 동일하게 gitignore 처리)에서 직접 읽는다.
    @PostConstruct
    public void initialize() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) return;

        try (InputStream credentials = new ClassPathResource("google-application.json").getInputStream()) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(credentials))
                    .build();
            FirebaseApp.initializeApp(options);
        }
    }
}
