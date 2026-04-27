package com.aicc.silverlink.domain.elderly.dto.response;

import com.aicc.silverlink.domain.elderly.entity.Elderly;
import com.aicc.silverlink.domain.system.entity.AdministrativeDivision;

import java.time.LocalDate;

public record ElderlySummaryResponse(
        Long userId,
        String name,
        String phone,
        // 행정구역 정보
        Long admCode,
        String sidoName,
        String sigunguName,
        String dongName,
        String fullAddress,
        // 기본 정보
        LocalDate birthDate,
        int age,
        Elderly.Gender gender,
        String addressLine1,
        String addressLine2,
        String zipcode,
        String guardianName,
        String counselorName) {
    public static ElderlySummaryResponse from(Elderly e) {
        return from(e, null, null);
    }

    public static ElderlySummaryResponse from(Elderly e, String guardianName) {
        return from(e, guardianName, null);
    }

    public static ElderlySummaryResponse from(Elderly e, String guardianName, String counselorName) {
        AdministrativeDivision division = e.getAdministrativeDivision();

        return new ElderlySummaryResponse(
                e.getId(),
                e.getUser().getName(),
                e.getUser().getPhone(),
                e.getAdmCode(),
                division != null ? division.getSidoName() : null,
                division != null ? division.getSigunguName() : null,
                division != null ? division.getDongName() : null,
                division != null ? division.getFullAddress() : null,
                e.getBirthDate(),
                e.age(),
                e.getGender(),
                e.getAddressLine1(),
                e.getAddressLine2(),
                e.getZipcode(),
                guardianName,
                counselorName);
    }
}