package com.hosose.waitingqueue;

import java.util.Set;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class WaitingQueueService {

    private final StringRedisTemplate redisTemplate;
    private final WaitingQueueProperties properties;

    // Redis에서 사용할 Key 정의
    private static final String ACTIVE_USER_SET_KEY = "waiting_queue:active_users";
    private static final String WAITING_QUEUE_ZSET_KEY = "waiting_queue:waiting_users";

    public WaitingQueueService(StringRedisTemplate redisTemplate, WaitingQueueProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    /**
     * 새로운 사용자의 서비스 진입을 시도합니다.
     * @return 발급된 사용자 토큰
     */
    public String tryEnter() {
        String userToken = UUID.randomUUID().toString();
        
        // 1. 현재 활성 사용자 수 확인
        Long activeUserCount = redisTemplate.opsForSet().size(ACTIVE_USER_SET_KEY);

        if (activeUserCount == null) {
            activeUserCount = 0L;
        }

        // 2. 허용 인원보다 적으면 바로 입장
        if (activeUserCount < properties.getMaxActiveUsers()) {
            addToActiveUsers(userToken);
        } else {
            // 3. 꽉 찼으면 대기열에 추가
            addToWaitingQueue(userToken);
        }
        return userToken;
    }

    /**
     * 특정 토큰이 활성 사용자인지 확인합니다.
     * @param userToken 사용자 토큰
     * @return 활성 사용자 여부
     */
    public boolean isActiveUser(String userToken) {
        return redisTemplate.opsForSet().isMember(ACTIVE_USER_SET_KEY, userToken);
    }
    
    /**
     * 사용자를 활성 사용자 Set에 추가합니다.
     * @param userToken 사용자 토큰
     */
    public void addToActiveUsers(String userToken) {
        redisTemplate.opsForSet().add(ACTIVE_USER_SET_KEY, userToken);
    }

    /**
     * 사용자를 대기열(Sorted Set)에 추가합니다. Score는 현재 시간입니다.
     * @param userToken 사용자 토큰
     */
    public void addToWaitingQueue(String userToken) {
        redisTemplate.opsForZSet().add(WAITING_QUEUE_ZSET_KEY, userToken, System.currentTimeMillis());
    }

    /**
     * 특정 사용자의 대기 순번을 조회합니다.
     * @param userToken 사용자 토큰
     * @return 대기 순번 (1부터 시작), 대기열에 없으면 -1
     */
    public Long getWaitingRank(String userToken) {
        Long rank = redisTemplate.opsForZSet().rank(WAITING_QUEUE_ZSET_KEY, userToken);
        return rank != null ? rank + 1 : -1;
    }
    
    /**
     * 대기열에서 다음 사용자들을 허용 인원만큼 입장시킵니다.
     * @param count 입장시킬 인원 수
     */
    public void allowNextUsers(long count) {
        if (count <= 0) {
            return;
        }

        // 1. 대기열에서 가장 오래된 사용자(score가 낮은)를 count만큼 가져옵니다.
        Set<String> usersToAllow = redisTemplate.opsForZSet().range(WAITING_QUEUE_ZSET_KEY, 0, count - 1);
        
        if (usersToAllow == null || usersToAllow.isEmpty()) {
            return;
        }

        // 2. 가져온 사용자들을 대기열에서 제거하고 활성 사용자 Set에 추가합니다.
        for (String userToken : usersToAllow) {
            redisTemplate.opsForZSet().remove(WAITING_QUEUE_ZSET_KEY, userToken);
            addToActiveUsers(userToken);
        }
    }

    /**
     * 전체 대기자 수를 조회합니다.
     * @return 전체 대기자 수
     */
    public Long getTotalWaitingCount() {
        Long count = redisTemplate.opsForZSet().size(WAITING_QUEUE_ZSET_KEY);
        return count != null ? count : 0;
    }
    
 // WaitingQueueService.java에 추가
    public Long getActiveUserCount() {
        Long count = redisTemplate.opsForSet().size(ACTIVE_USER_SET_KEY);
        return count != null ? count : 0;
    }
}