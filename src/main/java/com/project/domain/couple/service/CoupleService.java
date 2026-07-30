package com.project.domain.couple.service;

import com.project.domain.couple.dto.CoupleDTO;

public interface CoupleService {

    public void createInviteCode(Long userId);

    // 내 커플 연결 상태 조회 (연결 없음 -> null, 상대가 요청함 -> REQUESTED, 내가 요청함 -> WAIT, 연결됨 -> CONNECTED)
    public CoupleDTO getStatus(Long currentUserId);

    // 상대방 초대코드를 입력해서 연결을 요청/확정한다
    public CoupleDTO invite(Long currentUserId, String inviteCode);

    // 코드 없이, 지금 나한테 와 있는 요청을 그대로 수락한다
    public CoupleDTO accept(Long currentUserId);
}
