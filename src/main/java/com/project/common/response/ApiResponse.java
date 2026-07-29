package com.project.common.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {

    private final Integer status;
    private final String message;
    private final T data;

    public static <T> ApiResponse<T> success(Integer status, String message, T data) {
        return new ApiResponse<>(status, message, data);
    }

    public static <T> ApiResponse<T> error(Integer status, String message) {
        return new ApiResponse<>(status, message, null);
    }
}
