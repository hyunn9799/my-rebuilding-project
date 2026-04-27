package com.aicc.silverlink.domain.admin.service;

import com.aicc.silverlink.domain.admin.dto.request.AdminCreateRequest;
import com.aicc.silverlink.domain.admin.dto.request.AdminUpdateRequest;
import com.aicc.silverlink.domain.admin.dto.response.AdminResponse;
import com.aicc.silverlink.domain.admin.entity.Admin.AdminLevel;

import java.util.List;

/**
 * Admin Service 인터페이스
 */
public interface AdminService {

    /**
     * 관리자 생성
     */
    AdminResponse createAdmin(AdminCreateRequest request);

    /**
     * 관리자 정보 조회
     */
    AdminResponse getAdmin(Long userId);

    /**
     * 모든 관리자 조회
     */
    List<AdminResponse> getAllAdmins();

    /**
     * 행정구역 코드로 관리자 조회
     */
    List<AdminResponse> getAdminsByAdmCode(Long admCode);

    /**
     * 관리자 레벨로 조회
     */
    List<AdminResponse> getAdminsByLevel(AdminLevel adminLevel);

    /**
     * 특정 행정구역의 상위 관리자들 조회
     * 예: 역삼동의 상위 관리자 → 강남구, 서울시, 전국 관리자
     */
    List<AdminResponse> getSupervisors(Long admCode);

    /**
     * 특정 관리자의 하위 관리자들 조회
     * 예: 강남구 관리자의 하위 → 역삼동, 삼성동 등의 관리자
     */
    List<AdminResponse> getSubordinates(Long adminUserId);

    /**
     * 관리자가 특정 구역에 대한 권한이 있는지 확인
     */
    boolean hasJurisdiction(Long adminUserId, Long targetAdmCode);

    /**
     * 관리자 정보 수정 (담당 구역 변경)
     */
    AdminResponse updateAdmin(Long userId, AdminUpdateRequest request);

    /**
     * 관리자 삭제
     */
    void deleteAdmin(Long userId);
}