package com.project.domain.couple.mapper;

import com.project.domain.couple.dto.CoupleInvitationDTO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface CoupleInvitationMapper {

    // 현재 로그인한 사용자 조회
    public CoupleInvitationDTO findUserById(Long userId);

    // 나와 연결된(WAIT/CONNECTED) 모든 상대방 + 상태 목록 조회 (동시에 여러 명일 수 있음)
    public List<CoupleInvitationDTO> findCouplesByUserId(Long userId);

    // 나와 특정 상대(partnerId) 간의 커플 row 조회 (없으면 null)
    public CoupleInvitationDTO findCoupleByUserIds(@Param("userId") Long userId, @Param("partnerId") Long partnerId);

    // 초대코드 주인(상대방) 조회
    public CoupleInvitationDTO findPartnerByInviteCode(String inviteCode);

    // userId가 otherUserId 말고 다른 상대와 CONNECTED된 커플이 있는지 카운트 (WAIT는 여러 명과 동시에 있어도 됨)
    public int countCoupleExcept(@Param("userId") Long userId, @Param("otherUserId") Long otherUserId);

    // 신랑/신부 조합으로 기존 커플 상태 조회 (없으면 null)
    public String findCoupleStatus(@Param("groomId") Long groomId, @Param("brideId") Long brideId);

    // 커플 WAIT 상태로 최초 생성
    public void insertCouple(@Param("groomId") Long groomId, @Param("brideId") Long brideId);

    // WAIT -> CONNECTED로 전환
    public void connectCouple(@Param("groomId") Long groomId, @Param("brideId") Long brideId);

    // CONNECTED -> DISCONNECTED로 전환 (row는 삭제하지 않고 이력으로 남김)
    public void disconnectCouple(@Param("groomId") Long groomId, @Param("brideId") Long brideId);

    // groomId/brideId가 CONNECTED된 직후, 두 사람에게 걸려 있던 다른 WAIT 요청들을 삭제 (연결된 적이 없어서 이력으로 남길 필요가 없음)
    public void deleteOtherPendingRequests(@Param("groomId") Long groomId, @Param("brideId") Long brideId);

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
