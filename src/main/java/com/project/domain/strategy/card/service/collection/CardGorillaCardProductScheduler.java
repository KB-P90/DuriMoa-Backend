package com.project.domain.strategy.card.service.collection;

import com.project.domain.strategy.card.dto.CardProductCollectionResultDTO;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@RequiredArgsConstructor
public class CardGorillaCardProductScheduler {

    // 매일 02:00 KST에 카드고릴라 카드 상품을 자동 수집합니다.
    private static final String DAILY_COLLECTION_CRON = "0 0 2 * * *";

    // 배포 서버의 시스템 타임존과 무관하게 한국 시간 기준으로 실행합니다.
    private static final String SCHEDULER_ZONE = "Asia/Seoul";

    // 신용카드와 체크카드를 모두 수집합니다.
    private static final String CARD_TYPE_ALL = "ALL";

    private final CardGorillaCardProductCollectionService cardGorillaCardProductCollectionService;

    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(cron = DAILY_COLLECTION_CRON, zone = SCHEDULER_ZONE)
    public void collectDailyCardProducts() {
        if (!running.compareAndSet(false, true)) {
            log.warn("카드고릴라 카드 상품 자동 수집이 이미 실행 중이어서 이번 실행을 건너뜁니다.");
            return;
        }

        try {
            CardProductCollectionResultDTO result =
                    cardGorillaCardProductCollectionService.collectCardProducts(null, CARD_TYPE_ALL);
            log.info("카드고릴라 카드 상품 자동 수집을 완료했습니다. requestedCount={}, successCount={}, failureCount={}",
                    result.getRequestedCount(), result.getSuccessCount(), result.getFailureCount());
        } catch (Exception exception) {
            log.error("카드고릴라 카드 상품 자동 수집에 실패했습니다.", exception);
        } finally {
            running.set(false);
        }
    }
}
