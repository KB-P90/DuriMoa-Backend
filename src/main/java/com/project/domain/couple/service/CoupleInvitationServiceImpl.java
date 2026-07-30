package com.project.domain.couple.service;

import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.domain.couple.dto.CoupleInvitationDTO;
import com.project.domain.couple.mapper.CoupleInvitationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CoupleInvitationServiceImpl implements CoupleInvitationService {

    private static final String CODE_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    private final CoupleInvitationMapper coupleInvitationMapper;
    private final SecureRandom secureRandom = new SecureRandom();

    // 회원가입 직후 호출: 코드 중복될 때까지 재생성 후 30일 유효로 발급
    @Override
    @Transactional
    public void createInviteCode(Long userId) {
        String inviteCode;
        do {
            inviteCode = generateInviteCode();
        } while (coupleInvitationMapper.existsInviteCode(inviteCode));

        coupleInvitationMapper.insertInvitation(userId, inviteCode, LocalDateTime.now().plusDays(30));
    }

    // 내 커플 연결 상태 조회. WAIT인 경우 내 초대코드가 사용된 거면 REQUESTED(상대가 나한테 요청함)로 보여준다
    @Override
    public CoupleInvitationDTO getStatus(Long currentUserId) {
        CoupleInvitationDTO couple = coupleInvitationMapper.findCoupleByUserId(currentUserId);
        if (couple == null) {
            return null;
        }

        if ("WAIT".equals(couple.getStatus()) && coupleInvitationMapper.existsPendingRequestToMe(currentUserId)) {
            couple.setStatus("REQUESTED");
        }

        return couple;
    }

    // 상대방 초대코드를 입력해서 연결을 요청/확정한다.
    // 첫 번째 입력자는 couple을 WAIT로 생성하고, 두 번째 입력자(이미 WAIT인 커플을 발견)는 CONNECTED로 전환한다.
    @Override
    @Transactional
    public CoupleInvitationDTO invite(Long currentUserId, String inviteCode) {
        CoupleInvitationDTO currentUser = coupleInvitationMapper.findUserById(currentUserId);
        if (currentUser == null) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "존재하지 않는 회원입니다. userId=" + currentUserId);
        }

        CoupleInvitationDTO partner = coupleInvitationMapper.findPartnerByInviteCode(inviteCode);
        if (partner == null) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "유효하지 않은 초대코드입니다.");
        }

        validate(currentUser, partner);

        // 역할(G/B)에 따라 groom/bride 자리를 고정한다
        Long groomId;
        Long brideId;
        if ("G".equals(currentUser.getRole())) {
            groomId = currentUser.getUserId();
            brideId = partner.getUserId();
        } else {
            groomId = partner.getUserId();
            brideId = currentUser.getUserId();
        }

        String status = coupleInvitationMapper.findCoupleStatus(groomId, brideId);

        // 아직 아무도 연결 시도를 안 한 경우 -> WAIT로 최초 생성
        if (status == null) {
            coupleInvitationMapper.insertCouple(groomId, brideId);
            coupleInvitationMapper.requestInvitation(inviteCode, currentUserId);
            return new CoupleInvitationDTO(partner.getUserId(), partner.getName(), partner.getRole(), "WAIT");
        }

        // 상대방이 먼저 연결을 시도해 WAIT 상태인 경우 -> 지금 확정해서 CONNECTED로 전환
        if ("WAIT".equals(status)) {
            coupleInvitationMapper.connectCouple(groomId, brideId);
            coupleInvitationMapper.acceptInvitation(inviteCode, currentUserId);
            coupleInvitationMapper.acceptOwnInvitation(currentUserId);
            return new CoupleInvitationDTO(partner.getUserId(), partner.getName(), partner.getRole(), "CONNECTED");
        }

        // 이미 CONNECTED거나 그 외 상태면 재요청 불가
        throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "이미 연결된 커플입니다.");
    }

    // 코드 없이 버튼으로 수락: 내 초대코드로 이미 만들어진 WAIT 요청을 그대로 확정한다
    @Override
    @Transactional
    public CoupleInvitationDTO accept(Long currentUserId) {
        CoupleInvitationDTO currentUser = coupleInvitationMapper.findUserById(currentUserId);
        if (currentUser == null) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "존재하지 않는 회원입니다. userId=" + currentUserId);
        }

        CoupleInvitationDTO couple = coupleInvitationMapper.findCoupleByUserId(currentUserId);
        if (couple == null
                || !"WAIT".equals(couple.getStatus())
                || !coupleInvitationMapper.existsPendingRequestToMe(currentUserId)) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "대기 중인 연결 요청이 없습니다.");
        }

        Long groomId;
        Long brideId;
        if ("G".equals(currentUser.getRole())) {
            groomId = currentUser.getUserId();
            brideId = couple.getUserId();
        } else {
            groomId = couple.getUserId();
            brideId = currentUser.getUserId();
        }

        coupleInvitationMapper.connectCouple(groomId, brideId);
        coupleInvitationMapper.acceptOwnInvitation(currentUserId);

        return new CoupleInvitationDTO(couple.getUserId(), couple.getName(), couple.getRole(), "CONNECTED");
    }

    // 자기 자신 코드 입력 방지, 역할 동일 검사, 이미 다른 커플에 연결돼 있는지 검사
    private void validate(CoupleInvitationDTO currentUser, CoupleInvitationDTO partner) {
        if (currentUser.getUserId().equals(partner.getUserId())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "본인의 초대코드는 입력할 수 없습니다.");
        }

        if (currentUser.getRole().equals(partner.getRole())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "두 사용자의 역할이 동일합니다. 역할을 변경한 후 다시 시도해주세요.");
        }

        if (coupleInvitationMapper.countCoupleExcept(currentUser.getUserId(), partner.getUserId()) > 0
                || coupleInvitationMapper.countCoupleExcept(partner.getUserId(), currentUser.getUserId()) > 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "이미 다른 커플과 연결되어 있습니다.");
        }
    }

    // 영문 대문자+숫자 6자리 코드 생성 (예: ABC123)
    private String generateInviteCode() {
        StringBuilder code = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            code.append(CODE_CHARACTERS.charAt(secureRandom.nextInt(CODE_CHARACTERS.length())));
        }
        return code.toString();
    }
}
