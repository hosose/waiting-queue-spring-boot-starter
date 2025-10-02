package com.hosose.waitingqueue;

// 간단한 데이터 전달을 위해 record 사용 (Java 16+)
// 일반 클래스로 만들어도 무방합니다.
public record StatusResponse(String status, Long rank, Long totalWaiting) {
}