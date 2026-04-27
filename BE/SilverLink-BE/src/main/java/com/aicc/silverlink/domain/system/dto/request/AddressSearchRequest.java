package com.aicc.silverlink.domain.system.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주소 검색 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AddressSearchRequest {

    /**
     * 검색 타입
     */
    private SearchType searchType;

    /**
     * 전체 주소 문자열 (FULL_ADDRESS 타입)
     */
    private String fullAddress;

    /**
     * 시/도
     */
    private String sido;

    /**
     * 시/군/구
     */
    private String sigungu;

    /**
     * 도로명 (ROAD_ADDRESS 타입)
     */
    private String roadName;

    /**
     * 건물번호 (ROAD_ADDRESS 타입)
     */
    private Integer buildingNumber;

    /**
     * 읍/면/동 (JIBUN_ADDRESS 타입)
     */
    private String dong;

    /**
     * 지번 본번 (JIBUN_ADDRESS 타입)
     */
    private Integer jibunMain;

    /**
     * 지번 부번 (JIBUN_ADDRESS 타입)
     */
    private Integer jibunSub;

    /**
     * 우편번호 (ZIPCODE 타입)
     */
    private String zipcode;

    /**
     * 검색어 (KEYWORD 타입)
     */
    private String keyword;

    public enum SearchType {
        FULL_ADDRESS,   // 전체 주소 문자열
        ROAD_ADDRESS,   // 도로명주소
        JIBUN_ADDRESS,  // 지번주소
        ZIPCODE,        // 우편번호
        KEYWORD         // 검색어
    }

    // 편의 생성자들
    public static AddressSearchRequest byFullAddress(String fullAddress) {
        return new AddressSearchRequest(
                SearchType.FULL_ADDRESS, fullAddress,
                null, null, null, null, null, null, null, null, null
        );
    }

    public static AddressSearchRequest byRoadAddress(String sido, String sigungu,
                                                     String roadName, Integer buildingNumber) {
        return new AddressSearchRequest(
                SearchType.ROAD_ADDRESS, null,
                sido, sigungu, roadName, buildingNumber, null, null, null, null, null
        );
    }

    public static AddressSearchRequest byJibunAddress(String sido, String sigungu, String dong,
                                                      Integer jibunMain, Integer jibunSub) {
        return new AddressSearchRequest(
                SearchType.JIBUN_ADDRESS, null,
                sido, sigungu, null, null, dong, jibunMain, jibunSub, null, null
        );
    }

    public static AddressSearchRequest byZipcode(String zipcode) {
        return new AddressSearchRequest(
                SearchType.ZIPCODE, null,
                null, null, null, null, null, null, null, zipcode, null
        );
    }

    public static AddressSearchRequest byKeyword(String keyword) {
        return new AddressSearchRequest(
                SearchType.KEYWORD, null,
                null, null, null, null, null, null, null, null, keyword
        );
    }
}