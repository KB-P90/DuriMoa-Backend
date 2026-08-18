package com.project.domain.couple.service;

import com.project.common.enums.CoupleStatus;
import com.project.common.exception.CustomException;
import com.project.common.exception.ErrorCode;
import com.project.domain.couple.dto.CoupleInvitationDTO;
import com.project.domain.couple.mapper.CoupleInvitationMapper;
import com.project.domain.notification.dto.NotificationCreateRequestDTO;
import com.project.domain.notification.service.NotificationService;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Log4j2
@Service
@RequiredArgsConstructor
public class CoupleInvitationServiceImpl implements CoupleInvitationService {

    private static final String CODE_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final String TARGET_TYPE_COUPLE = "COUPLE";
    private static final String COUPLE_REQUESTED = "COUPLE_REQUESTED";
    private static final String COUPLE_ACCEPTED = "COUPLE_ACCEPTED";
    private static final String COUPLE_DISCONNECTED = "COUPLE_DISCONNECTED";
    private static final String COUPLE_REQUEST_TITLE = "커플 연결 요청";
    private static final String COUPLE_ACCEPTED_TITLE = "커플 연결 수락";
    private static final String COUPLE_DISCONNECTED_TITLE = "커플 연결이 해제되었습니다.";
    private static final String LOG_MARKER = "==============================";

    private final CoupleInvitationMapper coupleInvitationMapper;
    private final NotificationService notificationService;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    @Transactional
    public void createInviteCode(Long userId) {
        String inviteCode;
        do {
            inviteCode = generateInviteCode();
        } while (coupleInvitationMapper.existsInviteCode(inviteCode));

        coupleInvitationMapper.insertInvitation(userId, inviteCode, LocalDateTime.now().plusDays(30));
    }

    @Override
    public List<CoupleInvitationDTO> getStatuses(Long currentUserId) {
        return coupleInvitationMapper.findCouplesByUserId(currentUserId);
    }

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

        if (status == null) {
            coupleInvitationMapper.insertCouple(groomId, brideId);
            coupleInvitationMapper.requestInvitation(inviteCode, currentUserId);
            createCoupleRequestNotification(currentUser, partner);
            return new CoupleInvitationDTO(
                    partner.getUserId(),
                    partner.getName(),
                    partner.getRole(),
                    CoupleStatus.WAIT.getStatus()
            );
        }

        if (CoupleStatus.WAIT.getStatus().equals(status)) {
            coupleInvitationMapper.connectCouple(groomId, brideId);
            coupleInvitationMapper.acceptInvitation(inviteCode, currentUserId);
            coupleInvitationMapper.acceptOwnInvitation(currentUserId, partner.getUserId());
            coupleInvitationMapper.deleteOtherPendingRequests(groomId, brideId);
            coupleInvitationMapper.deleteOtherPendingInvitations(currentUserId, partner.getUserId());
            createCoupleAcceptedNotification(partner, currentUser);
            return new CoupleInvitationDTO(
                    partner.getUserId(),
                    partner.getName(),
                    partner.getRole(),
                    CoupleStatus.CONNECTED.getStatus()
            );
        }

        throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "이미 연결된 커플입니다.");
    }

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
        createCoupleAcceptedNotification(couple, currentUser);

        return new CoupleInvitationDTO(
                couple.getUserId(),
                couple.getName(),
                couple.getRole(),
                CoupleStatus.CONNECTED.getStatus()
        );
    }

    @Override
    @Transactional
    public CoupleInvitationDTO disconnect(Long currentUserId) {
        CoupleInvitationDTO currentUser = coupleInvitationMapper.findUserById(currentUserId);
        if (currentUser == null) {
            throw new CustomException(ErrorCode.RESOURCE_NOT_FOUND, "존재하지 않는 회원입니다. userId=" + currentUserId);
        }

        CoupleInvitationDTO couple = coupleInvitationMapper.findCouplesByUserId(currentUserId).stream()
                .filter(candidate -> CoupleStatus.CONNECTED.getStatus().equals(candidate.getStatus()))
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
        createCoupleDisconnectedNotification(couple, currentUser);

        return new CoupleInvitationDTO(
                couple.getUserId(),
                couple.getName(),
                couple.getRole(),
                CoupleStatus.DISCONNECTED.getStatus()
        );
    }

    private void validate(CoupleInvitationDTO currentUser, CoupleInvitationDTO partner) {
        if (currentUser.getUserId().equals(partner.getUserId())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "본인의 초대코드는 입력할 수 없습니다.");
        }

        if (currentUser.getRole().equals(partner.getRole())) {
            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "두 사용자의 역할이 동일합니다.");
        }


        int currentUserCoupleCount = coupleInvitationMapper.countCoupleExcept(
                currentUser.getUserId(),
                partner.getUserId()
        );

        int partnerCoupleCount = coupleInvitationMapper.countCoupleExcept(
                partner.getUserId(),
                currentUser.getUserId()
        );

        if (currentUserCoupleCount > 0 || partnerCoupleCount > 0) {

            throw new CustomException(ErrorCode.INVALID_INPUT_VALUE, "이미 다른 커플과 연결되어 있습니다.");
        }
    }

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
                COUPLE_REQUESTED,
                COUPLE_REQUEST_TITLE,
                requester.getName() + "님이 커플 연결을 요청했습니다.",
                requester.getUserId()
        );
    }

    private void createCoupleDisconnectedNotification(
            CoupleInvitationDTO receiver,
            CoupleInvitationDTO partner
    ) {
        createNotification(
                receiver.getUserId(),
                COUPLE_DISCONNECTED,
                COUPLE_DISCONNECTED_TITLE,
                partner.getName() + "님과의 커플 연결이 해제되었습니다.",
                partner.getUserId()
        );
    }

    private void createCoupleAcceptedNotification(
            CoupleInvitationDTO requester,
            CoupleInvitationDTO acceptor
    ) {
        createNotification(
                requester.getUserId(),
                COUPLE_ACCEPTED,
                COUPLE_ACCEPTED_TITLE,
                acceptor.getName() + "님이 커플 연결 요청을 수락했습니다.",
                acceptor.getUserId()
        );
    }

    private void createNotification(
            Long userId,
            String type,
            String title,
            String content,
            Long targetId
    ) {
        NotificationCreateRequestDTO request = NotificationCreateRequestDTO.builder()
                .userId(userId)
                .type(type)
                .title(title)
                .content(content)
                .targetType(TARGET_TYPE_COUPLE)
                .targetId(targetId)
                .build();

        if (TransactionSynchronizationManager.isSynchronizationActive()) {

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendNotification(request);
                }
            });
            return;
        }

        sendNotification(request);
    }

    private void sendNotification(NotificationCreateRequestDTO request) {
        try {
            notificationService.createNotification(request);

        } catch (RuntimeException exception) {
            log.info("{} 커플 알림 생성 예외 발생 - userId: {}, type: {}, targetId: {}, exceptionType: {}, message: {}",
                    LOG_MARKER,
                    request.getUserId(), request.getType(), request.getTargetId(), exception);
        }
    }
}
