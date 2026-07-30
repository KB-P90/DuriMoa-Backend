package com.project.domain.home.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class HomeException extends RuntimeException {

    // 홈 도메인 오류를 HTTP 상태로 변환할 때 사용하는 상태값입니다.
    private final HttpStatus status;

    // 홈 도메인 공통 예외 상태와 메시지를 초기화합니다.
    protected HomeException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public static class HomeAuthenticationRequiredException extends HomeException {

        // 홈 조회에 필요한 인증 사용자 정보를 찾지 못했을 때 사용합니다.
        public HomeAuthenticationRequiredException() {
            super(HttpStatus.UNAUTHORIZED, "인증 사용자 정보를 확인할 수 없습니다.");
        }
    }

    public static class HomeUserNotFoundException extends HomeException {

        // 인증 사용자 ID에 해당하는 활성 사용자가 없을 때 사용합니다.
        public HomeUserNotFoundException() {
            super(HttpStatus.NOT_FOUND, "사용자 정보를 찾을 수 없습니다.");
        }
    }
}
