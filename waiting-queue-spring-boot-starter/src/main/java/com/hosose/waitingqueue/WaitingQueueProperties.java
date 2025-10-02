package com.hosose.waitingqueue;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 대기열 라이브러리의 설정을 담는 클래스
 * 사용자는 application.yml 또는 .properties 파일에서 이 값들을 설정할 수 있습니다.
 */
@ConfigurationProperties(prefix = "waiting-queue") // "waiting-queue"로 시작하는 설정을 매핑
public class WaitingQueueProperties {

    /**
     * 대기열 기능 활성화 여부 (기본값: false)
     */
    private boolean enabled = false;

    /**
     * 서비스에 동시 접속을 허용할 최대 인원 수
     */
    private Long maxActiveUsers = 1000L;

    /**
     * 사용자 토큰을 저장할 쿠키의 이름
     */
    private String tokenCookieName = "wq_token";
    
    /**
     * 대기 중인 사용자를 리다이렉트 시킬 대기 페이지 URL
     */
    private String waitingPageUrl = "/waiting.html";

    // --- Getter와 Setter ---
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Long getMaxActiveUsers() {
        return maxActiveUsers;
    }

    public void setMaxActiveUsers(Long maxActiveUsers) {
        this.maxActiveUsers = maxActiveUsers;
    }

    public String getTokenCookieName() {
        return tokenCookieName;
    }

    public void setTokenCookieName(String tokenCookieName) {
        this.tokenCookieName = tokenCookieName;
    }

    public String getWaitingPageUrl() {
        return waitingPageUrl;
    }

    public void setWaitingPageUrl(String waitingPageUrl) {
        this.waitingPageUrl = waitingPageUrl;
    }
}