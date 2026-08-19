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

class SignupRequestDTOTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void validSignupFieldsPassValidation() throws Exception {
        SignupRequestDTO request =
                createRequest(
                        "010-1234-5678", "password123", "password123", "홍길동", "G");

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void invalidSignupFieldsFailValidation() throws Exception {
        SignupRequestDTO request = createRequest("123", "short", "", "", "X");

        Set<ConstraintViolation<SignupRequestDTO>> violations = validator.validate(request);

        assertEquals(6, violations.size());
    }

    @Test
    void nullTermsAgreementFieldsFailValidation() throws Exception {
        SignupRequestDTO request =
                createRequest(
                        "010-1234-5678", "password123", "password123", "홍길동", "G");
        setField(request, "serviceTermsAgreed", null);
        setField(request, "privacyTermsAgreed", null);
        setField(request, "financeTermsAgreed", null);

        Set<ConstraintViolation<SignupRequestDTO>> violations = validator.validate(request);

        assertEquals(3, violations.size());
    }

    @Test
    void falseRequiredTermsAgreementFieldsFailValidation() throws Exception {
        String[] requiredTermsFields = {
            "serviceTermsAgreed", "privacyTermsAgreed", "financeTermsAgreed"
        };

        for (String fieldName : requiredTermsFields) {
            SignupRequestDTO request =
                    createRequest(
                            "010-1234-5678", "password123", "password123", "홍길동", "G");
            setField(request, fieldName, false);

            Set<ConstraintViolation<SignupRequestDTO>> violations = validator.validate(request);

            assertEquals(1, violations.size(), fieldName);
        }
    }

    private SignupRequestDTO createRequest(
            String phone,
            String password,
            String passwordConfirm,
            String name,
            String role) throws Exception {
        SignupRequestDTO request = new SignupRequestDTO();
        setField(request, "phone", phone);
        setField(request, "password", password);
        setField(request, "passwordConfirm", passwordConfirm);
        setField(request, "name", name);
        setField(request, "role", role);
        setField(request, "serviceTermsAgreed", true);
        setField(request, "privacyTermsAgreed", true);
        setField(request, "financeTermsAgreed", true);
        return request;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
