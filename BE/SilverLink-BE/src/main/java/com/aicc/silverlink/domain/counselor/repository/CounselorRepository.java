package com.aicc.silverlink.domain.counselor.repository;

import com.aicc.silverlink.domain.counselor.entity.Counselor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CounselorRepository extends JpaRepository<Counselor, Long> {

    @Query("SELECT c FROM Counselor c " +
            "JOIN FETCH c.user " +
            "JOIN FETCH c.administrativeDivision " +
            "WHERE c.id = :id")
    Optional<Counselor> findByIdWithUser(@Param("id") Long id);

    @Query("SELECT c FROM Counselor c " +
            "JOIN FETCH c.user " +
            "JOIN FETCH c.administrativeDivision")
    List<Counselor> findAllWithUser();

    /**
     * 행정구역 코드로 상담사 목록 조회
     */
    @Query("SELECT c FROM Counselor c " +
            "JOIN FETCH c.user " +
            "JOIN FETCH c.administrativeDivision ad " +
            "WHERE ad.admCode = :admCode")
    List<Counselor> findByAdmCode(@Param("admCode") Long admCode);

    /**
     * 특정 시/도에 속한 상담사 목록 조회
     */
    @Query("SELECT c FROM Counselor c " +
            "JOIN FETCH c.user " +
            "JOIN FETCH c.administrativeDivision ad " +
            "WHERE ad.sidoCode = :sidoCode")
    List<Counselor> findBySidoCode(@Param("sidoCode") String sidoCode);

    /**
     * 특정 시/군/구에 속한 상담사 목록 조회
     */
    @Query("SELECT c FROM Counselor c " +
            "JOIN FETCH c.user " +
            "JOIN FETCH c.administrativeDivision ad " +
            "WHERE ad.sidoCode = :sidoCode AND ad.sigunguCode = :sigunguCode")
    List<Counselor> findBySigungu(
            @Param("sidoCode") String sidoCode,
            @Param("sigunguCode") String sigunguCode
    );
}