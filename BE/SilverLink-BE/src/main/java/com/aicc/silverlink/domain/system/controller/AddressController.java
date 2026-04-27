package com.aicc.silverlink.domain.system.controller;

import com.aicc.silverlink.domain.system.dto.response.AddressResponse;
import com.aicc.silverlink.domain.system.service.AddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "주소", description = "행정구역 주소 조회 API")
@Slf4j
@RestController
@RequestMapping("/api/address")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    /**
     * 시/도 목록 조회
     * GET /api/address/sido
     */
    @GetMapping("/sido")
    public ResponseEntity<List<AddressResponse>> getAllSido() {
        log.info("GET /api/address/sido");
        return ResponseEntity.ok(addressService.getAllSido());
    }

    /**
     * 시/군/구 목록 조회
     * GET /api/address/sigungu?sidoCode=11
     */
    @GetMapping("/sigungu")
    public ResponseEntity<List<AddressResponse>> getSigungu(@RequestParam String sidoCode) {
        log.info("GET /api/address/sigungu?sidoCode={}", sidoCode);
        return ResponseEntity.ok(addressService.getSigunguBySido(sidoCode));
    }

    /**
     * 읍/면/동 목록 조회
     * GET /api/address/dong?sidoCode=11&sigunguCode=680
     */
    @GetMapping("/dong")
    public ResponseEntity<List<AddressResponse>> getDong(
            @RequestParam String sidoCode,
            @RequestParam String sigunguCode) {
        log.info("GET /api/address/dong?sidoCode={}&sigunguCode={}", sidoCode, sigunguCode);
        return ResponseEntity.ok(addressService.getDongBySigungu(sidoCode, sigunguCode));
    }

    /**
     * 행정동 코드로 주소 조회
     * GET /api/address/{admCode}
     */
    @GetMapping("/{admCode}")
    public ResponseEntity<AddressResponse> getAddress(@PathVariable Long admCode) {
        log.info("GET /api/address/{}", admCode);
        return ResponseEntity.ok(addressService.getAddressByAdmCode(admCode));
    }
}