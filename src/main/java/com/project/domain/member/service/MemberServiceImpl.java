package com.project.domain.member.service;

import com.project.domain.member.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberMapper memberMapper;

    @Override
    @Transactional(readOnly = true)
    public boolean existsMemberById(Long userId) {
        return userId != null && memberMapper.existsMemberById(userId);
    }
}
