package com.project.domain.couple.service;

import com.project.domain.couple.dto.CoupleInvitationDTO;

import java.util.List;

public interface CoupleInvitationService {

    public void createInviteCode(Long userId);

    // 나와 연결된(WAIT/CONNECTED) 모든 상대방 + 상태 목록 조회 (동시에 여러 명의 요청이 올 수 있음)
    public List<CoupleInvitationDTO> getStatuses(Long currentUserId);

    // 상대방 초대코드를 입력해서 연결을 요청/확정한다
    public CoupleInvitationDTO invite(Long currentUserId, String inviteCode);

    // 나에게 와 있는 여러 요청 중 특정 상대(partnerUserId)의 요청을 수락한다
    public CoupleInvitationDTO accept(Long currentUserId, Long partnerUserId);

    // 연결된 커플을 끊는다 (row는 삭제하지 않고 DISCONNECTED로 이력만 남김)
    public CoupleInvitationDTO disconnect(Long currentUserId);
}
