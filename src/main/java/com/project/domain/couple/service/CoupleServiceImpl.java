package com.project.domain.couple.service;

import com.project.common.exception.CoupleAccessDeniedException;
import com.project.common.exception.CoupleNotConnectedException;
import com.project.common.exception.CoupleNotFoundException;
import com.project.common.exception.UserNotFoundException;
import com.project.common.security.CurrentUserProvider;
import com.project.domain.couple.dto.CoupleDTO;
import com.project.domain.couple.mapper.CoupleMapper;
import com.project.domain.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CoupleServiceImpl implements CoupleService {

    private static final String CONNECTED_STATUS = "CONNECTED";

    private final CoupleMapper coupleMapper;
    private final MemberService memberService;
    private final CurrentUserProvider currentUserProvider;

    @Override
    public CoupleDTO getConnectedCoupleForCurrentUser() {
        return getConnectedCoupleByUserId(currentUserProvider.getCurrentUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public CoupleDTO getConnectedCoupleByUserId(Long userId) {
        if (userId == null) {
            throw new UserNotFoundException(null);
        }

        if (!memberService.existsMemberById(userId)) {
            throw new UserNotFoundException(userId);
        }

        List<CoupleDTO> couples = coupleMapper.findCouplesByUserId(userId);
        if (couples == null || couples.isEmpty()) {
            throw new CoupleNotFoundException(userId);
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
            throw new CoupleNotConnectedException(userId);
        }

        throw new CoupleAccessDeniedException(userId, null);
    }
}
