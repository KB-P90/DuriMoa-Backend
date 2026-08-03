package com.project.domain.couple.service;

import com.project.common.enums.CoupleStatus;
import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.domain.couple.dto.CoupleInvitationDTO;
import com.project.domain.couple.mapper.CoupleInvitationMapper;
import com.project.domain.notification.dto.NotificationCreateRequestDTO;
import com.project.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CoupleInvitationServiceImpl implements CoupleInvitationService {

    private static final String CODE_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final String COUPLE_NOTIFICATION_TYPE = "COUPLE";
    private static final String COUPLE_REQUEST_TITLE = "커플 연결 요청";
    private static final String COUPLE_CONNECTED_TITLE = "커플 연결 완료";

    private final CoupleInvitationMapper coupleInvitationMapper;
    private final NotificationService notificationService;
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

    // 나와 연결된(WAIT/CONNECTED) 모든 상대방 목록 조회. 동시에 여러 명에게서 요청이 와 있을 수 있다.
    @Override
    public List<CoupleInvitationDTO> getStatuses(Long currentUserId) {
        return coupleInvitationMapper.findCouplesByUserId(currentUserId);
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
            createCoupleRequestNotification(currentUser, partner);
            return new CoupleInvitationDTO(partner.getUserId(), partner.getName(), partner.getRole(), CoupleStatus.WAIT.getStatus());
        }

        // 상대방이 먼저 연결을 시도해 WAIT 상태인 경우 -> 지금 확정해서 CONNECTED로 전환
        if (CoupleStatus.WAIT.getStatus().equals(status)) {
            coupleInvitationMapper.connectCouple(groomId, brideId);
            coupleInvitationMapper.acceptInvitation(inviteCode, currentUserId);
            coupleInvitationMapper.acceptOwnInvitation(currentUserId, partner.getUserId());
            coupleInvitationMapper.deleteOtherPendingRequests(groomId, brideId);
            coupleInvitationMapper.deleteOtherPendingInvitations(currentUserId, partner.getUserId());
            createCoupleConnectedNotifications(currentUser, partner);
            return new CoupleInvitationDTO(partner.getUserId(), partner.getName(), partner.getRole(), CoupleStatus.CONNECTED.getStatus());
        }

        // 이미 CONNECTED거나 그 외 상태면 재요청 불가
        throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "이미 연결된 커플입니다.");
    }

    // 나에게 와 있는 여러 요청 중 특정 상대(partnerUserId)의 요청을 수락한다.
    // 이미 다른 상대와 CONNECTED된 상태라면(예: 다른 요청을 먼저 수락한 경우) 남은 요청은 수락할 수 없다.
    @Override
    @Transactional
    public CoupleInvitationDTO accept(Long currentUserId, Long partnerUserId) {
        CoupleInvitationDTO currentUser = coupleInvitationMapper.findUserById(currentUserId);
        if (currentUser == null) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "존재하지 않는 회원입니다. userId=" + currentUserId);
        }

        CoupleInvitationDTO couple = coupleInvitationMapper.findCoupleByUserIds(currentUserId, partnerUserId);
        if (couple == null || !CoupleStatus.WAIT.getStatus().equals(couple.getStatus())) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "대기 중인 연결 요청이 없습니다.");
        }

        if (coupleInvitationMapper.countCoupleExcept(currentUserId, partnerUserId) > 0
                || coupleInvitationMapper.countCoupleExcept(partnerUserId, currentUserId) > 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "이미 다른 커플과 연결되어 있습니다.");
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
        coupleInvitationMapper.acceptOwnInvitation(currentUserId, partnerUserId);
        coupleInvitationMapper.deleteOtherPendingRequests(groomId, brideId);
        coupleInvitationMapper.deleteOtherPendingInvitations(currentUserId, partnerUserId);
        createCoupleConnectedNotification(couple, currentUser);

        return new CoupleInvitationDTO(couple.getUserId(), couple.getName(), couple.getRole(), CoupleStatus.CONNECTED.getStatus());
    }

    // 연결된 커플을 끊는다. row는 지우지 않고 DISCONNECTED로 남겨서, 이후 새 상대와는 자유롭게 연결할 수 있게 한다.
    @Override
    @Transactional
    public CoupleInvitationDTO disconnect(Long currentUserId) {
        CoupleInvitationDTO currentUser = coupleInvitationMapper.findUserById(currentUserId);
        if (currentUser == null) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "존재하지 않는 회원입니다. userId=" + currentUserId);
        }

        CoupleInvitationDTO couple = coupleInvitationMapper.findCouplesByUserId(currentUserId).stream()
                .filter(c -> CoupleStatus.CONNECTED.getStatus().equals(c.getStatus()))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "연결된 커플이 없습니다."));

        Long groomId;
        Long brideId;
        if ("G".equals(currentUser.getRole())) {
            groomId = currentUser.getUserId();
            brideId = couple.getUserId();
        } else {
            groomId = couple.getUserId();
            brideId = currentUser.getUserId();
        }

        coupleInvitationMapper.disconnectCouple(groomId, brideId);

        return new CoupleInvitationDTO(couple.getUserId(), couple.getName(), couple.getRole(), CoupleStatus.DISCONNECTED.getStatus());
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

    private void createCoupleRequestNotification(
            CoupleInvitationDTO requester,
            CoupleInvitationDTO receiver
    ) {
        createNotification(
                receiver.getUserId(),
                COUPLE_REQUEST_TITLE,
                requester.getName() + "님이 커플 연결을 요청했습니다."
        );
    }

    private void createCoupleConnectedNotifications(
            CoupleInvitationDTO currentUser,
            CoupleInvitationDTO partner
    ) {
        createCoupleConnectedNotification(currentUser, partner);
        createCoupleConnectedNotification(partner, currentUser);
    }

    private void createCoupleConnectedNotification(
            CoupleInvitationDTO receiver,
            CoupleInvitationDTO partner
    ) {
        createNotification(
                receiver.getUserId(),
                COUPLE_CONNECTED_TITLE,
                partner.getName() + "님과 커플 연결이 완료되었습니다."
        );
    }

    private void createNotification(Long userId, String title, String content) {
        notificationService.createNotification(
                NotificationCreateRequestDTO.builder()
                        .userId(userId)
                        .type(COUPLE_NOTIFICATION_TYPE)
                        .title(title)
                        .content(content)
                        .build()
        );
    }
}
