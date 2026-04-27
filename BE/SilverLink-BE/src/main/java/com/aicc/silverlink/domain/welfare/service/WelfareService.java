package com.aicc.silverlink.domain.welfare.service;

import com.aicc.silverlink.domain.welfare.dto.*;
import com.aicc.silverlink.domain.welfare.entity.Source;
import com.aicc.silverlink.domain.welfare.entity.Welfare;
import com.aicc.silverlink.domain.welfare.repository.WelfareRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import org.springframework.http.HttpStatusCode;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class WelfareService {

    private final WelfareRepository welfareRepository;
    private final ModelMapper modelMapper;

    // [변경] RestClient → WebClient (비동기/논블로킹)
    private final WebClient webClient;

    @Value("${api.welfare.service-key}")
    private String serviceKey;

    @Value("${api.welfare.central-url}")
    private String centralUrl;

    @Value("${api.welfare.local-url}")
    private String localUrl;

    @Value("${api.welfare.central-detail-url}")
    private String centralDetailUrl;

    @Value("${api.welfare.local-detail-url}")
    private String localDetailUrl;

    // =================================================================================
    // [1] 데이터 조회 로직
    // =================================================================================

    @Transactional(readOnly = true)
    public Page<WelfareListResponse> searchWelfare(WelfareSearchRequest request, Pageable pageable) {
        Page<Welfare> welfarePage = welfareRepository.searchByKeyword(request.getKeyword(), pageable);
        return welfarePage.map(welfare -> modelMapper.map(welfare, WelfareListResponse.class));
    }

    @Cacheable(value = "welfare", key = "#welfareId", unless = "#result == null")
    @Transactional(readOnly = true)
    public WelfareDetailResponse getWelfareDetail(Long welfareId) {
        Welfare welfare = welfareRepository.findById(welfareId)
                .orElseThrow(() -> new IllegalArgumentException("해당 복지 서비스를 찾을 수 없습니다. ID=" + welfareId));
        return modelMapper.map(welfare, WelfareDetailResponse.class);
    }

    // =================================================================================
    // [2] 데이터 수집 및 동기화 로직 (WebClient 비동기 방식)
    // =================================================================================

    @Scheduled(cron = "0 0 4 * * *")
    public void syncAllWelfareScheduled() {
        log.info("=== [스케줄러 시작] 노인 복지 데이터 전수 동기화 (WebClient) ===");
        long startTime = System.currentTimeMillis();

        // 비동기 병렬 처리로 두 소스 동시 동기화
        Mono.when(
                syncCentralWelfareDataAsync(),
                syncLocalWelfareDataAsync()).block(); // 스케줄러에서는 완료 대기

        long endTime = System.currentTimeMillis();
        log.info("=== [스케줄러 종료] 소요 시간: {}ms ===", (endTime - startTime));
    }

    @Transactional
    public void syncCentralWelfareData() {
        syncCentralWelfareDataAsync().block();
    }

    @Transactional
    public void syncLocalWelfareData() {
        syncLocalWelfareDataAsync().block();
    }

    /**
     * 중앙 복지 데이터 비동기 동기화
     */
    public Mono<Void> syncCentralWelfareDataAsync() {
        return syncDataWithPaginationAsync(centralUrl, WelfareApiDto.CentralResponse.class, Source.CENTRAL);
    }

    /**
     * 지자체 복지 데이터 비동기 동기화
     */
    public Mono<Void> syncLocalWelfareDataAsync() {
        return syncDataWithPaginationAsync(localUrl, WelfareApiDto.LocalResponse.class, Source.LOCAL);
    }

    /**
     * WebClient를 사용한 비동기 페이징 데이터 수집
     * - 논블로킹 방식으로 병렬 처리
     * - flatMap을 통한 동시 요청 처리
     */
    private <T, R extends WelfareApiDto.ResponseWrapper<T>> Mono<Void> syncDataWithPaginationAsync(
            String baseUrl, Class<R> responseType, Source source) {

        AtomicInteger totalCount = new AtomicInteger(0);
        log.info("[{}] WebClient 비동기 데이터 수집 시작...", source);

        return fetchAllPagesAsync(baseUrl, responseType, source, 1, new ArrayList<>())
                .flatMap(items -> {
                    totalCount.addAndGet(items.size());
                    // 병렬로 저장 처리 (최대 10개 동시)
                    return Flux.fromIterable(items)
                            .parallel()
                            .runOn(Schedulers.boundedElastic())
                            .flatMap(item -> Mono.fromRunnable(() -> saveOrUpdate(item, source)))
                            .sequential()
                            .then();
                })
                .doOnSuccess(v -> log.info("[{}] 총 {}건 동기화 완료", source, totalCount.get()))
                .doOnError(e -> log.error("[{}] 동기화 중 오류 발생: {}", source, e.getMessage()));
    }

    /**
     * 재귀적으로 모든 페이지 데이터 수집
     */
    private <T, R extends WelfareApiDto.ResponseWrapper<T>> Mono<List<T>> fetchAllPagesAsync(
            String baseUrl, Class<R> responseType, Source source, int pageNo, List<T> accumulated) {

        int numOfRows = 100;

        // serviceKey가 인코딩된 상태(% 포함)라면 디코딩하여 UriComponentsBuilder가 다시 인코딩하도록 함
        // serviceKey가 인코딩된 상태(% 포함)라면 디코딩하여 UriComponentsBuilder가 다시 인코딩하도록 함
        String finalServiceKey = serviceKey;
        if (serviceKey != null) {
            String trimmedKey = serviceKey.trim();
            if (trimmedKey.contains("%")) {
                try {
                    finalServiceKey = URLDecoder.decode(trimmedKey, StandardCharsets.UTF_8);
                } catch (Exception e) {
                    log.warn("Error decoding service key: {}", e.getMessage());
                    finalServiceKey = trimmedKey;
                }
            } else {
                finalServiceKey = trimmedKey;
            }
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("serviceKey", finalServiceKey)
                .queryParam("numOfRows", numOfRows)
                .queryParam("pageNo", pageNo)
                .queryParam("lifeArray", "006");

        if (source == Source.CENTRAL) {
            builder.queryParam("callTp", "L");
            builder.queryParam("srchKeyCode", "001");
        }

        URI uri = builder.build().toUri();
        log.info("[{}] Request URI: {}", source, uri); // 디버깅용 로그

        return webClient.get()
                .uri(uri)
                .retrieve()
                .onStatus(HttpStatusCode::isError, response -> response.bodyToMono(String.class)
                        .flatMap(body -> {
                            log.error("[{}] API Error: Status={}, Body={}", source, response.statusCode(), body);
                            return Mono.error(new RuntimeException(response.statusCode() + " " + body));
                        }))
                .bodyToMono(responseType)
                .timeout(Duration.ofSeconds(30))
                .flatMap(response -> {
                    if (response == null || response.getServList() == null || response.getServList().isEmpty()) {
                        log.info("[{}] 페이지 {} - 데이터 없음, 수집 종료", source, pageNo);
                        return Mono.just(accumulated);
                    }

                    List<T> items = response.getServList();
                    accumulated.addAll(items);
                    log.debug("[{}] 페이지 {} - {}건 수집", source, pageNo, items.size());

                    if (items.size() < numOfRows) {
                        log.info("[{}] 마지막 페이지 도달 (페이지: {}, 누적: {}건)", source, pageNo, accumulated.size());
                        return Mono.just(accumulated);
                    }

                    // 다음 페이지 재귀 호출
                    return fetchAllPagesAsync(baseUrl, responseType, source, pageNo + 1, accumulated);
                })
                .onErrorResume(e -> {
                    log.error("[{}] 페이지 {} 요청 실패: {}", source, pageNo, e.getMessage());
                    return Mono.just(accumulated); // 에러 시 현재까지 수집된 데이터 반환
                });
    }

    private <T> void saveOrUpdate(T item, Source source) {
        // 1. 노년 필터링
        if (item instanceof WelfareApiDto.CentralItem c) {
            if (c.getLifeArray() == null || !c.getLifeArray().contains("노년"))
                return;
        } else if (item instanceof WelfareApiDto.LocalItem l) {
            if (l.getLifeNmArray() == null || !l.getLifeNmArray().contains("노년"))
                return;
        }

        String servId = getServIdFromItem(item);

        // 2. 엔티티 조회 또는 생성
        Welfare welfare = welfareRepository.findByServId(servId).orElseGet(Welfare::new);

        // 3. 매핑 (Config에 설정된 규칙대로 자동 변환!)
        modelMapper.map(item, welfare);

        // 4. 상세 정보 병합 (비동기)
        fetchAndMergeDetailAsync(servId, welfare, source).block();

        // 5. 공통 필드 및 저장
        welfare.setSource(source);
        welfareRepository.save(welfare);
    }

    /**
     * 상세 정보 비동기 조회 및 병합
     */
    private Mono<Void> fetchAndMergeDetailAsync(String servId, Welfare welfare, Source source) {
        String detailUrl = (source == Source.CENTRAL) ? centralDetailUrl : localDetailUrl;

        // serviceKey 스마트 처리
        // serviceKey 스마트 처리
        String finalServiceKey = serviceKey;
        if (serviceKey != null) {
            String trimmedKey = serviceKey.trim();
            if (trimmedKey.contains("%")) {
                try {
                    finalServiceKey = URLDecoder.decode(trimmedKey, StandardCharsets.UTF_8);
                } catch (Exception e) {
                    /* ignore */ }
            } else {
                finalServiceKey = trimmedKey;
            }
        }

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(detailUrl)
                .queryParam("serviceKey", finalServiceKey)
                .queryParam("servId", servId);

        if (source == Source.CENTRAL) {
            builder.queryParam("callTp", "D");
        }

        URI uri = builder.build().toUri();
        log.info("[{}] Detail URI: {}", source, uri);

        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(WelfareApiDto.DetailItem.class)
                .timeout(Duration.ofSeconds(10))
                .doOnSuccess(detail -> {
                    if (detail != null) {
                        if (detail.getAlwServCn() != null)
                            welfare.setAlwServCn(detail.getAlwServCn());
                        if (detail.getSlctCritCn() != null)
                            welfare.setSlctCritCn(detail.getSlctCritCn());

                        if (detail.getTgtrDtlCn() != null)
                            welfare.setTargetDtlCn(detail.getTgtrDtlCn()); // 중앙
                        if (detail.getSprtTrgtCn() != null)
                            welfare.setTargetDtlCn(detail.getSprtTrgtCn()); // 지자체

                        // 문의처가 상세에만 있는 경우 보완
                        if (welfare.getRprsCtadr() == null && detail.getRprsCtadr() != null) {
                            welfare.setRprsCtadr(detail.getRprsCtadr());
                        }
                    }
                })
                .onErrorResume(e -> {
                    log.warn("상세 정보 수집 실패 (ID: {}): {}", servId, e.getMessage());
                    return Mono.empty();
                })
                .then();
    }

    private String getServIdFromItem(Object item) {
        if (item instanceof WelfareApiDto.CentralItem c)
            return c.getServId();
        if (item instanceof WelfareApiDto.LocalItem l)
            return l.getServId();
        return null;
    }
}