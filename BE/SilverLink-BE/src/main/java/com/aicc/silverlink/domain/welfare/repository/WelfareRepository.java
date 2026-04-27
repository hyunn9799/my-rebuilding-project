package com.aicc.silverlink.domain.welfare.repository;

import com.aicc.silverlink.domain.welfare.entity.Source;
import com.aicc.silverlink.domain.welfare.entity.Welfare;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WelfareRepository extends JpaRepository<Welfare, Long> {

    // servId로 중복 체크 및 업데이트를 위해 조회
    Optional<Welfare> findByServId(String servId);

    // 저장 전 존재 여부만 빠르게 확인
    boolean existsByServId(String servId);

    // [수정 포인트]
    // 1. SELECT w FROM Welfare w (소문자 w로 통일)
    // 2. 줄 끝마다 공백(" ") 추가 (SQL이 안 붙게 방지)
    // 3. True -> true (소문자가 정석)
    // 4. Like -> LIKE (대문자가 정석)
    @Query("SELECT w FROM Welfare w " +
            "WHERE w.isActive = true " +
            "AND (:source IS NULL OR w.source = :source) " +
            "AND (:districtCode IS NULL OR w.districtCode = :districtCode) " +
            "AND (:category IS NULL OR w.category = :category) " +
            "AND (:keyword IS NULL OR w.servNm LIKE %:keyword% OR w.servDgst LIKE %:keyword%)")
    Page<Welfare> searchWelfare(
            @Param("keyword") String keyword,
            @Param("districtCode") String districtCode,
            @Param("category") String category,
            @Param("source") Source source,
            Pageable pageable
    );
    @Query("SELECT w FROM Welfare w " +
            "WHERE (:keyword IS NULL OR :keyword = '' OR w.servNm LIKE %:keyword% OR w.servDgst LIKE %:keyword%) " +
            "AND w.isActive = true")
    Page<Welfare> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}