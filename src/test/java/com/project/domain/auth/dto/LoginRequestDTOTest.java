package com.project.domain.auth.dto;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.lang.reflect.Field;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoginRequestDTOTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void validLoginFieldsPassValidation() throws Exception {
        LoginRequestDTO request = createRequest("010-1234-5678", "password123");

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void invalidLoginFieldsFailValidation() throws Exception {
        LoginRequestDTO request = createRequest("123", "short");

        Set<ConstraintViolation<LoginRequestDTO>> violations = validator.validate(request);

        assertEquals(2, violations.size());
    }

    private LoginRequestDTO createRequest(String phone, String password) throws Exception {
        LoginRequestDTO request = new LoginRequestDTO();
        setField(request, "phone", phone);
        setField(request, "password", password);
        return request;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
