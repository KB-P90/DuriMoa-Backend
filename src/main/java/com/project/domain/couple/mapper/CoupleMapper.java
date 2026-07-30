package com.project.domain.couple.mapper;

import com.project.domain.couple.dto.CoupleDTO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

public interface CoupleMapper {

    // 현재 로그인한 사용자 조회
    public CoupleDTO findUserById(Long userId);

    // 나와 연결된(WAIT/CONNECTED) 커플의 상대방 + 상태 조회 (없으면 null)
    public CoupleDTO findCoupleByUserId(Long userId);

    // 내 초대코드를 누가 사용해서, 내가 확인해줘야 하는 요청이 와 있는지 확인
    public boolean existsPendingRequestToMe(Long userId);

    // 초대코드 주인(상대방) 조회
    public CoupleDTO findPartnerByInviteCode(String inviteCode);

    // userId가 otherUserId 말고 다른 상대와 연결된 커플이 있는지 카운트
    public int countCoupleExcept(@Param("userId") Long userId, @Param("otherUserId") Long otherUserId);

    // 신랑/신부 조합으로 기존 커플 상태 조회 (없으면 null)
    public String findCoupleStatus(@Param("groomId") Long groomId, @Param("brideId") Long brideId);

    // 커플 WAIT 상태로 최초 생성
    public void insertCouple(@Param("groomId") Long groomId, @Param("brideId") Long brideId);

    // WAIT -> CONNECTED로 전환
    public void connectCouple(@Param("groomId") Long groomId, @Param("brideId") Long brideId);

    // 상대방 초대코드를 처음 사용했을 때 REQUESTED로 변경
    public void requestInvitation(@Param("inviteCode") String inviteCode, @Param("inviteeId") Long inviteeId);

    // 상대방 초대코드를 최종 확정했을 때 ACCEPTED로 변경
    public void acceptInvitation(@Param("inviteCode") String inviteCode, @Param("inviteeId") Long inviteeId);

    // 연결이 확정되면 내 초대코드(REQUESTED 상태)도 ACCEPTED로 변경
    public void acceptOwnInvitation(Long inviterId);

    // 초대코드 생성 시 중복 여부 확인용
    public boolean existsInviteCode(String inviteCode);

    // 회원가입 시 초대코드 발급
    public void insertInvitation(
            @Param("inviterId") Long inviterId,
            @Param("inviteCode") String inviteCode,
            @Param("expiredAt") LocalDateTime expiredAt);
}
