package com.aicc.silverlink.global.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // Common (공통 에러)
    INVALID_INPUT_VALUE(400, "C001", "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(500, "C002", "서버 내부 오류입니다."),
    HANDLE_ACCESS_DENIED(403, "C003", "접근 권한이 없습니다."),
    ENTITY_NOT_FOUND(404, "C004", "해당 엔티티를 찾을 수 없습니다."),
    ACCESS_DENIED(403, "C005", "접근이 거부되었습니다."),
    DUPLICATE_RESOURCE(409, "C006", "이미 존재하는 리소스입니다."),

    // User / Auth (사용자 및 인증 관련)
    USER_NOT_FOUND(404, "U001", "사용자를 찾을 수 없습니다."),
    LOGIN_FAILED(401, "U002", "아이디 또는 비밀번호가 일치하지 않습니다."),
    DUPLICATE_LOGIN_ID(400, "U003", "이미 존재하는 아이디입니다."),

    // Business (비즈니스 로직 관련)
    ASSIGNMENT_ALREADY_EXISTS(400, "A001", "이미 배정된 상담사-어르신 관계입니다."),

    // Call Review (통화 리뷰 관련)
    CALL_RECORD_NOT_FOUND(404, "CR001", "통화 기록을 찾을 수 없습니다."),
    REVIEW_NOT_FOUND(404, "CR002", "리뷰를 찾을 수 없습니다."),
    REVIEW_ALREADY_EXISTS(409, "CR003", "이미 확인한 통화 기록입니다."),
    NOT_ASSIGNED_ELDERLY(403, "CR004", "담당하지 않는 어르신입니다."),
    NOT_RELATED_ELDERLY(403, "CR005", "보호 관계가 없는 어르신입니다.");

    private final int status;
    private final String code;
    private final String message;

    ErrorCode(int status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }
}