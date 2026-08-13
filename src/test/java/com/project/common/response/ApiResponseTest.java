package com.project.common.response;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class ApiResponseTest {

    @Test
    void errorReturnsErrorCodeAndMessage() {
        ApiResponse<Void> response = ApiResponse.error("C001", "잘못된 입력값입니다.");

        assertFalse(response.getSuccess());
        assertEquals("C001", response.getErrorCode());
        assertEquals("잘못된 입력값입니다.", response.getMessage());
        assertNull(response.getData());
    }

    @Test
    void errorCanReturnOnlySuccessStatusAndMessage() {
        ApiResponse<Void> response = ApiResponse.error("오류가 발생했습니다.");

        assertFalse(response.getSuccess());
        assertNull(response.getErrorCode());
        assertEquals("오류가 발생했습니다.", response.getMessage());
        assertNull(response.getData());
    }
}
