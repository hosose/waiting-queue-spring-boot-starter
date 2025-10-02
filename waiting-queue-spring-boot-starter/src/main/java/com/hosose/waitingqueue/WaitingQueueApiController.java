package com.hosose.waitingqueue;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/waiting-queue")
public class WaitingQueueApiController {

    private final WaitingQueueService waitingQueueService;
    private final WaitingQueueProperties properties;

    public WaitingQueueApiController(WaitingQueueService waitingQueueService, WaitingQueueProperties properties) {
        this.waitingQueueService = waitingQueueService;
		this.properties = new WaitingQueueProperties();
    }

    @GetMapping("/status")
    public ResponseEntity<StatusResponse> getStatus(
            @CookieValue(name = "${waiting-queue.token-cookie-name:wq_token}", required = false) String token) {

        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        // Case 1: 이미 활성 사용자가 된 경우
        if (waitingQueueService.isActiveUser(token)) {
            return ResponseEntity.ok(new StatusResponse("ACTIVE", 0L, 0L));
        }

        // Case 2: 아직 대기 중인 경우
        Long rank = waitingQueueService.getWaitingRank(token);
        if (rank > 0) {
            Long totalWaiting = waitingQueueService.getTotalWaitingCount();
            return ResponseEntity.ok(new StatusResponse("WAITING", rank, totalWaiting));
        }

        // Case 3: 유효하지 않은 토큰인 경우
        return ResponseEntity.status(401).build(); // Unauthorized
    }
}