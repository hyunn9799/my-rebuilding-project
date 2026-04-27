package com.aicc.silverlink.domain.system.repository;

import com.aicc.silverlink.domain.system.entity.AdministrativeDivision;
import com.aicc.silverlink.domain.system.entity.AdministrativeDivision.DivisionLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AdministrativeDivisionRepository extends JpaRepository<AdministrativeDivision, Long> {

    /**
     * 시/도 목록 조회
     */
    @Query("SELECT a FROM AdministrativeDivision a WHERE a.level = 'SIDO' AND a.isActive = true ORDER BY a.sidoCode")
    List<AdministrativeDivision> findAllSido();

    /**
     * 특정 시/도의 시/군/구 목록
     */
    @Query("""
            SELECT a FROM AdministrativeDivision a
            WHERE a.level = 'SIGUNGU'
            AND a.sidoCode = :sidoCode
            AND a.isActive = true
            ORDER BY a.sigunguCode
            """)
    List<AdministrativeDivision> findSigunguBySido(@Param("sidoCode") String sidoCode);

    /**
     * 특정 시/군/구의 읍/면/동 목록
     */
    @Query("""
            SELECT a FROM AdministrativeDivision a
            WHERE a.level = 'DONG'
            AND a.sidoCode = :sidoCode
            AND a.sigunguCode = :sigunguCode
            AND a.isActive = true
            ORDER BY a.dongCode
            """)
    List<AdministrativeDivision> findDongBySigungu(
            @Param("sidoCode") String sidoCode,
            @Param("sigunguCode") String sigunguCode);

    /**
     * 시도코드와 시군구코드로 첫 번째 매칭 조회 (법정동 코드 매핑용)
     */
    @Query("""
            SELECT a FROM AdministrativeDivision a
            WHERE a.sidoCode = :sidoCode
            AND (:sigunguCode IS NULL OR a.sigunguCode = :sigunguCode)
            AND a.isActive = true
            ORDER BY a.level DESC
            """)
    List<AdministrativeDivision> findBySidoAndSigungu(
            @Param("sidoCode") String sidoCode,
            @Param("sigunguCode") String sigunguCode);

    /**
     * 기본 행정구역 조회 (fallback용)
     */
    @Query("SELECT a FROM AdministrativeDivision a WHERE a.isActive = true ORDER BY a.admCode LIMIT 1")
    java.util.Optional<AdministrativeDivision> findAnyActive();
}