package com.hosose.waitingqueue;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;

public class WaitingQueueInterceptor implements HandlerInterceptor {

    private final Logger log = LoggerFactory.getLogger(WaitingQueueInterceptor.class);
    private final WaitingQueueService waitingQueueService;
    private final WaitingQueueProperties properties;

    public WaitingQueueInterceptor(WaitingQueueService waitingQueueService, WaitingQueueProperties properties) {
        this.waitingQueueService = waitingQueueService;
        this.properties = properties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        // 1. 쿠키에서 대기열 토큰을 찾습니다.
        Optional<String> tokenOpt = findTokenFromCookie(request);

        // Case 1: 토큰이 없는 신규 사용자
        if (tokenOpt.isEmpty()) {
            String newToken = waitingQueueService.tryEnter();
            addTokenToCookie(response, newToken);

            // 바로 활성 사용자가 되었다면 통과
            if (waitingQueueService.isActiveUser(newToken)) {
                log.info("New user entered. Token: {}", newToken);
                return true;
            }
            
            // 대기열로 갔다면 대기 페이지로 리다이렉트
            log.info("New user added to waiting queue. Token: {}", newToken);
            response.sendRedirect(properties.getWaitingPageUrl());
            return false;
        }

        // Case 2: 토큰이 있는 기존 사용자
        String token = tokenOpt.get();
        if (waitingQueueService.isActiveUser(token)) {
            // 활성 사용자라면 통과
            log.trace("Active user re-entered. Token: {}", token);
            return true;
        }

        // 활성 사용자가 아니라면 대기열에 있는지 확인 (혹시 모를 엣지 케이스)
        if (waitingQueueService.getWaitingRank(token) > 0) {
            // 대기자라면 대기 페이지로 리다이렉트
            log.trace("Waiting user re-entered. Token: {}", token);
            response.sendRedirect(properties.getWaitingPageUrl());
            return false;
        }

        // 유효하지 않은 토큰(만료되었거나 잘못된 토큰)을 가진 경우, 신규 사용자로 취급
        log.warn("Invalid token detected. Treating as a new user. Invalid Token: {}", token);
        // 여기서 신규 사용자 로직을 다시 태우거나, 에러 페이지로 보내는 등의 처리가 가능합니다.
        // 여기서는 간단히 신규 사용자 진입 로직을 다시 실행합니다.
        String newToken = waitingQueueService.tryEnter();
        addTokenToCookie(response, newToken);
        if(waitingQueueService.isActiveUser(newToken)) {
             return true;
        }
        response.sendRedirect(properties.getWaitingPageUrl());
        return false;
    }

    private Optional<String> findTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> cookie.getName().equals(properties.getTokenCookieName()))
                .map(Cookie::getValue)
                .findFirst();
    }

    private void addTokenToCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(properties.getTokenCookieName(), token);
        cookie.setPath("/"); // 모든 경로에서 쿠키가 유효하도록 설정
        // cookie.setMaxAge(3600); // 쿠키 유효시간 설정 (예: 1시간)
        response.addCookie(cookie);
    }
}