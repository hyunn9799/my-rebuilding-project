package com.aicc.silverlink.domain.system.service;

import com.aicc.silverlink.domain.system.dto.response.AddressResponse;
import com.aicc.silverlink.domain.system.entity.AdministrativeDivision;
import com.aicc.silverlink.domain.system.repository.AdministrativeDivisionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AddressServiceImpl implements AddressService {

    private final AdministrativeDivisionRepository repository;

    @Cacheable(value = "divisions", key = "'sido:all'")
    @Override
    public List<AddressResponse> getAllSido() {
        log.info("전체 시/도 목록 조회");
        return repository.findAllSido().stream()
                .map(AddressResponse::from)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "divisions", key = "'sigungu:' + #sidoCode")
    @Override
    public List<AddressResponse> getSigunguBySido(String sidoCode) {
        log.info("시/도 {}의 시/군/구 목록 조회", sidoCode);
        return repository.findSigunguBySido(sidoCode).stream()
                .map(AddressResponse::from)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "divisions", key = "'dong:' + #sidoCode + ':' + #sigunguCode")
    @Override
    public List<AddressResponse> getDongBySigungu(String sidoCode, String sigunguCode) {
        log.info("시/군/구 {}-{}의 읍/면/동 목록 조회", sidoCode, sigunguCode);
        return repository.findDongBySigungu(sidoCode, sigunguCode).stream()
                .map(AddressResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    public AddressResponse getAddressByAdmCode(Long admCode) {
        log.info("행정동 코드 {}로 주소 조회", admCode);
        AdministrativeDivision division = repository.findById(admCode)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 행정동 코드: " + admCode));
        return AddressResponse.from(division);
    }
}
