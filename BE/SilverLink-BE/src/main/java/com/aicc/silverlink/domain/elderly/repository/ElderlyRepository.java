package com.aicc.silverlink.domain.elderly.repository;

import com.aicc.silverlink.domain.elderly.entity.Elderly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ElderlyRepository extends JpaRepository<Elderly, Long> {

    @Query("SELECT e FROM Elderly e " +
            "JOIN FETCH e.user " +
            "JOIN FETCH e.administrativeDivision " +
            "WHERE e.id = :id")
    Optional<Elderly> findWithUserById(@Param("id") Long id);

    boolean existsById(Long id);

    /**
     * 행정구역 코드로 어르신 목록 조회
     */
    @Query("SELECT e FROM Elderly e " +
            "JOIN FETCH e.user " +
            "JOIN FETCH e.administrativeDivision ad " +
            "WHERE ad.admCode = :admCode")
    List<Elderly> findByAdmCode(@Param("admCode") Long admCode);

    /**
     * ✅ 전체 어르신 목록 조회 (관리자용: User 및 행정구역 정보 포함)
     */
    @Query("SELECT e FROM Elderly e " +
            "JOIN FETCH e.user " +
            "JOIN FETCH e.administrativeDivision")
    List<Elderly> findAllWithUserAndDivision();

    /**
     * ✅ 특정 시/도에 속한 어르신 목록 조회
     */
    @Query("SELECT e FROM Elderly e " +
            "JOIN FETCH e.user " +
            "JOIN FETCH e.administrativeDivision ad " +
            "WHERE ad.sidoCode = :sidoCode")
    List<Elderly> findBySidoCode(@Param("sidoCode") String sidoCode);

    /**
     * 현재 시간/요일에 통화 예정인 어르신 목록 (CallBot용)
     */
    @Query("SELECT e FROM Elderly e " +
            "JOIN FETCH e.user u " +
            "WHERE e.callScheduleEnabled = true " +
            "AND e.preferredCallTime = :time " +
            "AND e.preferredCallDays LIKE %:dayCode%")
    List<Elderly> findDueForCall(@Param("time") String time, @Param("dayCode") String dayCode);

    /**
     * 통화 스케줄이 활성화된 전체 어르신 목록 조회
     */
    @Query("SELECT e FROM Elderly e " +
            "JOIN FETCH e.user " +
            "WHERE e.callScheduleEnabled = true")
    List<Elderly> findAllWithCallScheduleEnabled();

    /**
     * 이름으로 어르신 검색 (관리자용)
     */
    List<Elderly> findAllByUser_NameContaining(String name);
}