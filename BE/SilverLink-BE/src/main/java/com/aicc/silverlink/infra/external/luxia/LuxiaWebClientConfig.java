package com.aicc.silverlink.infra.external.luxia;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class LuxiaWebClientConfig {

        // Document AI는 이미지 처리라 오래 걸릴 수 있음 (기본 90초)
        private static final int DEFAULT_CONNECT_TIMEOUT_MS = 10000;
        private static final int DEFAULT_RESPONSE_TIMEOUT_MS = 90000;

        @Bean
        public WebClient luxiaWebClient(LuxiaProperties props) {
                // 프로퍼티에서 타임아웃 값 로드 (없으면 기본값 사용)
                int connectMs = DEFAULT_CONNECT_TIMEOUT_MS;
                int responseMs = DEFAULT_RESPONSE_TIMEOUT_MS;

                try {
                        if (props.timeout() != null) {
                                connectMs = props.timeout().connectMs() > 0 ? props.timeout().connectMs()
                                                : DEFAULT_CONNECT_TIMEOUT_MS;
                                responseMs = props.timeout().responseMs() > 0 ? props.timeout().responseMs()
                                                : DEFAULT_RESPONSE_TIMEOUT_MS;
                        }
                } catch (Exception e) {
                        log.warn("Failed to load Luxia timeout config, using defaults: connectMs={}, responseMs={}",
                                        DEFAULT_CONNECT_TIMEOUT_MS, DEFAULT_RESPONSE_TIMEOUT_MS);
                }

                log.info("Luxia WebClient configured: baseUrl={}, connectMs={}, responseMs={}",
                                props.baseUrl(), connectMs, responseMs);

                final int finalConnectMs = connectMs;
                final int finalResponseMs = responseMs;

                HttpClient httpClient = HttpClient.create()
                                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, finalConnectMs)
                                .responseTimeout(Duration.ofMillis(finalResponseMs))
                                .doOnConnected(conn -> conn
                                                .addHandlerLast(new ReadTimeoutHandler(finalResponseMs,
                                                                TimeUnit.MILLISECONDS))
                                                .addHandlerLast(new WriteTimeoutHandler(finalResponseMs,
                                                                TimeUnit.MILLISECONDS)));

                return WebClient.builder()
                                .baseUrl(props.baseUrl())
                                .clientConnector(new ReactorClientHttpConnector(httpClient))
                                .defaultHeader("apikey", props.apiKey())
                                .build();
        }

}
