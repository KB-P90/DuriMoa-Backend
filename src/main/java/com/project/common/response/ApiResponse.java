package com.project.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {

    // 요청 처리 성공 여부를 화면과 공통 예외 처리에서 함께 사용합니다.
    private final Boolean success;

    // HTTP 상태 코드를 응답 본문에도 전달합니다.
    private final Integer status;

    // 사용자에게 전달할 처리 결과 메시지입니다.
    private final String message;

    // API별 실제 응답 데이터입니다.
    private final T data;

    // 정상 응답 본문을 공통 형식으로 감쌉니다.
    public static <T> ApiResponse<T> success(Integer status, String message, T data) {
        return new ApiResponse<>(true, status, message, data);
    }

    // 오류 응답 본문을 공통 형식으로 감쌉니다.
    public static ApiResponse<Void> failure(Integer status, String message) {
        return new ApiResponse<>(false, status, message, null);
    }
}
