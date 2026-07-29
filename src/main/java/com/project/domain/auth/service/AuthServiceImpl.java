package com.project.domain.auth.service;

import com.project.domain.auth.dto.SignupRequestDTO;
import com.project.domain.auth.dto.SignupResponseDTO;
import com.project.domain.auth.exception.PasswordConfirmationMismatchException;
import com.project.domain.auth.exception.PhoneDuplicateException;
import com.project.domain.auth.mapper.AuthMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthMapper authMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public SignupResponseDTO signup(SignupRequestDTO request) {
        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new PasswordConfirmationMismatchException();
        }

        String normalizedPhone = request.getPhone().replace("-", "");
        if (Boolean.TRUE.equals(authMapper.existsUserByPhone(normalizedPhone))) {
            throw new PhoneDuplicateException();
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        try {
            authMapper.insertUser(
                    normalizedPhone,
                    encodedPassword,
                    request.getName(),
                    request.getRole());
        } catch (DuplicateKeyException exception) {
            throw new PhoneDuplicateException();
        }

        return authMapper.findSignedUpUserByPhone(normalizedPhone);
    }
}
