package com.project.domain.couple.service;

import com.project.domain.couple.dto.CoupleDTO;
import com.project.domain.couple.mapper.CoupleMapper;
import com.project.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CoupleServiceImpl implements CoupleService {

    private static final String CONNECTED_STATUS = "CONNECTED";

    private final CoupleMapper coupleMapper;
    private final MemberService memberService;

    @Override
    public CoupleDTO getConnectedCoupleForCurrentUser() {
        return getConnectedCoupleByUserId(getCurrentUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public CoupleDTO getConnectedCoupleByUserId(Long userId) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.");
        }

        if (!memberService.existsMemberById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.");
        }

        List<CoupleDTO> couples = coupleMapper.findCouplesByUserId(userId);
        if (couples == null || couples.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "연결된 커플을 찾을 수 없습니다.");
        }

        boolean hasNonConnectedCouple = false;
        for (CoupleDTO couple : couples) {
            if (!couple.hasMember(userId)) {
                continue;
            }
            if (CONNECTED_STATUS.equals(couple.getStatus())) {
                return couple;
            }
            hasNonConnectedCouple = true;
        }

        if (hasNonConnectedCouple) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "연결된 커플만 이용할 수 있습니다.");
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "커플 구성원만 이용할 수 있습니다.");
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증된 사용자 정보가 필요합니다.");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Number) {
            return ((Number) principal).longValue();
        }

        if (principal instanceof String) {
            try {
                return Long.parseLong((String) principal);
            } catch (NumberFormatException exception) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "인증된 사용자 정보가 필요합니다.", exception);
            }
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증된 사용자 정보가 필요합니다.");
    }
}
