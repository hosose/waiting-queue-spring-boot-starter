package com.hosose.waitingqueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

public class WaitingQueueScheduler {

    private final Logger log = LoggerFactory.getLogger(WaitingQueueScheduler.class);
    private final WaitingQueueService waitingQueueService;
    private final WaitingQueueProperties properties;

    public WaitingQueueScheduler(WaitingQueueService waitingQueueService, WaitingQueueProperties properties) {
        this.waitingQueueService = waitingQueueService;
        this.properties = properties;
    }

    // 1초(1000ms)마다 주기적으로 실행
    @Scheduled(fixedRate = 1000)
    public void processQueue() {
        log.trace("Running waiting queue scheduler...");
        
        // 1. 현재 활성 사용자 수 확인
        long activeUserCount = waitingQueueService.getActiveUserCount(); // 이 메소드는 Service에 추가 필요
        
        // 2. 입장 가능한 인원 수 계산
        long availableSlots = properties.getMaxActiveUsers() - activeUserCount;
        
        if (availableSlots > 0) {
            log.debug("Found {} available slots. Allowing next users.", availableSlots);
            // 3. 대기열에서 다음 사용자 입장 처리
            waitingQueueService.allowNextUsers(availableSlots);
        }
    }
}