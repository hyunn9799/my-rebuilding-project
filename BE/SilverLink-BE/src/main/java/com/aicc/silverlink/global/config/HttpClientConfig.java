package com.aicc.silverlink.global.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;

import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * HTTP 클라이언트 설정
 * - RestClient: 동기 방식 (기존)
 * - WebClient: 비동기/논블로킹 방식 (신규)
 */
@Configuration
public class HttpClientConfig {

    /**
     * WebClient Bean (비동기/논블로킹 방식 - 성능 개선)
     * - Connection Pool 설정
     * - Timeout 설정
     * - 메모리 버퍼 제한 설정
     */
    @Bean
    @Primary
    public WebClient webClient() {
        // Connection Pool 설정
        ConnectionProvider provider = ConnectionProvider.builder("welfare-pool")
                .maxConnections(500) // 최대 연결 수
                .maxIdleTime(Duration.ofSeconds(20)) // 유휴 연결 유지 시간
                .maxLifeTime(Duration.ofSeconds(60)) // 연결 최대 생존 시간
                .pendingAcquireTimeout(Duration.ofSeconds(60)) // 연결 대기 타임아웃
                .evictInBackground(Duration.ofSeconds(120)) // 백그라운드 정리 주기
                .build();

        // HttpClient 설정 (Netty 기반)
        HttpClient httpClient = HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000) // 연결 타임아웃 10초
                .responseTimeout(Duration.ofSeconds(30)) // 응답 타임아웃 30초
                .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(30, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(30, TimeUnit.SECONDS)));

        DefaultUriBuilderFactory factory = new DefaultUriBuilderFactory();
        factory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.NONE);

        return WebClient.builder()
                .uriBuilderFactory(factory)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(10 * 1024 * 1024)) // 10MB 버퍼
                .build();
    }
}
