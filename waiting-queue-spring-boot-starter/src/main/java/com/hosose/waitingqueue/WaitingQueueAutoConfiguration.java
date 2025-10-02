package com.hosose.waitingqueue;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@AutoConfiguration
@ConditionalOnProperty(name = "waiting-queue.enabled", havingValue = "true")
@EnableConfigurationProperties(WaitingQueueProperties.class)
public class WaitingQueueAutoConfiguration {

	@Bean
	public WaitingQueueService waitingQueueService(StringRedisTemplate redisTemplate, WaitingQueueProperties properties) {
		return new WaitingQueueService(redisTemplate, properties);
	}

	// --- 5단계에서 만든 Interceptor를 Bean으로 등록 ---
	@Bean
	public WaitingQueueInterceptor waitingQueueInterceptor(WaitingQueueService waitingQueueService, WaitingQueueProperties properties) {
		return new WaitingQueueInterceptor(waitingQueueService, properties);
	}

	// --- 6단계: Interceptor를 Spring MVC에 등록하는 설정 ---
	@Bean
	public WebMvcConfigurer waitingQueueMvcConfigurer(WaitingQueueInterceptor interceptor, WaitingQueueProperties properties) {
		return new WebMvcConfigurer() {
			@Override
			public void addInterceptors(InterceptorRegistry registry) {
				registry.addInterceptor(interceptor).addPathPatterns("/**") // 모든 경로에 인터셉터를 적용
						.excludePathPatterns("/error", properties.getWaitingPageUrl(), "/*.css", "/*.js", "/*.ico"); // 특정 경로는 제외
			}
		};
	}

	@Bean
	public WaitingQueueScheduler waitingQueueScheduler(WaitingQueueService waitingQueueService, WaitingQueueProperties properties) {
		return new WaitingQueueScheduler(waitingQueueService, properties);
	}

	@Bean
	public WaitingQueueApiController waitingQueueApiController(WaitingQueueService waitingQueueService, WaitingQueueProperties properties) {
		return new WaitingQueueApiController(waitingQueueService, properties);
	}
}