package com.aicc.silverlink.domain.map.repository;

import com.aicc.silverlink.domain.map.entity.WelfareFacility;
import com.aicc.silverlink.domain.map.entity.WelfareFacilityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WelfareFacilityRepository extends JpaRepository<WelfareFacility, Long> {

    // 현재 위치(latitude, longitude)를 기준으로 반경(radiusKm) 내에 있는 시설을 조회하는 쿼리
    // 이 쿼리는 MySQL의 ST_Distance_Sphere 함수를 사용합니다.
    // 다른 DB (PostgreSQL, H2 등)를 사용한다면 해당 DB의 지리 정보 함수로 변경해야 합니다.
    @Query(value = "SELECT * FROM welfare_facilities wf " +
            "WHERE ST_Distance_Sphere(POINT(wf.longitude, wf.latitude), POINT(:userLongitude, :userLatitude)) <= :radiusKm * 1000",
            nativeQuery = true)
    List<WelfareFacility> findFacilitiesWithinRadius(
            @Param("userLatitude") Double userLatitude,
            @Param("userLongitude") Double userLongitude,
            @Param("radiusKm") Double radiusKm
    );

    // 필요하다면 시설 타입별 검색 등 추가 쿼리 메서드를 정의할 수 있습니다.
    List<WelfareFacility> findByType(WelfareFacilityType type);
}
