package com.aicc.silverlink.domain.guardian.repository;

import com.aicc.silverlink.domain.guardian.entity.Guardian;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GuardianRepository extends JpaRepository<Guardian, Long> {

    @Query("SELECT g FROM Guardian g JOIN FETCH g.user WHERE g.id = :id")
    Optional<Guardian> findByIdWithUser(@Param("id") Long id);

    @Query("SELECT g FROM Guardian g JOIN FETCH g.user")
    List<Guardian> findAllWithUser();
}