package com.aicc.silverlink.domain.system.service;

import com.aicc.silverlink.domain.system.dto.response.AddressResponse;

import java.util.List;

public interface AddressService {

    /**
     * 시/도 목록 조회
     */
    List<AddressResponse> getAllSido();

    /**
     * 특정 시/도의 시/군/구 목록 조회
     */
    List<AddressResponse> getSigunguBySido(String sidoCode);

    /**
     * 특정 시/군/구의 읍/면/동 목록 조회
     */
    List<AddressResponse> getDongBySigungu(String sidoCode, String sigunguCode);

    /**
     * 행정동 코드로 주소 정보 조회
     */
    AddressResponse getAddressByAdmCode(Long admCode);
}