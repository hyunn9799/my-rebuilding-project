package com.aicc.silverlink.global.exception;

import com.aicc.silverlink.global.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 데이터 무결성 오류 (DB 제약조건 위반)
     */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrityViolation(
            org.springframework.dao.DataIntegrityViolationException e) {
        log.error("DataIntegrityViolationException: {}", e.getMessage());

        String message = "데이터 저장 중 오류가 발생했습니다.";
        String rootCause = e.getRootCause() != null ? e.getRootCause().getMessage() : "";

        // 일반적인 제약 조건 오류 메시지 변환
        if (rootCause.contains("Duplicate entry")) {
            if (rootCause.contains("phone")) {
                message = "이미 등록된 전화번호입니다.";
            } else if (rootCause.contains("login_id")) {
                message = "이미 사용 중인 로그인 ID입니다.";
            } else if (rootCause.contains("email")) {
                message = "이미 등록된 이메일입니다.";
            } else {
                message = "중복된 데이터가 존재합니다.";
            }
        } else if (rootCause.contains("doesn't have a default value")) {
            message = "필수 입력 항목이 누락되었습니다. 관리자에게 문의해주세요.";
        } else if (rootCause.contains("foreign key")) {
            message = "연결된 데이터를 찾을 수 없습니다. 입력 값을 확인해주세요.";
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "DATA_INTEGRITY_ERROR", "message", message));
    }

    /**
     * 비즈니스 로직 처리 중 발생하는 예외를 한곳에서 처리합니다.
     */
    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ApiResponse<?>> handleBusinessException(BusinessException e) {
        log.error("handleBusinessException: {}", e.getErrorCode().getMessage());
        ErrorCode errorCode = e.getErrorCode();

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode.getMessage()));
    }

    /**
     * 예상치 못한 일반적인 모든 예외를 처리합니다.
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResponse<?>> handleException(Exception e) {
        log.error("handleException", e);

        return ResponseEntity
                .status(500)
                .body(ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }

    /**
     * 로그인 실패 / 잘못된 인자 (IllegalArgumentException)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException e) {
        String code = e.getMessage();
        String message = translateErrorCode(code);

        if ("LOGIN_FAIL".equals(code)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED) // 401
                    .body(Map.of("error", code, "message", message));
        }
        
        // NO_TOKEN 예외 처리 추가
        if ("NO_TOKEN".equals(code)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST) // 400
                    .body(Map.of("error", code, "message", "토큰이 없습니다."));
        }

        log.warn("IllegalArgumentException: code={}, message={}", code, message);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST) // 400
                .body(Map.of("error", code, "message", message));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<String> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException e,
            HttpServletRequest req) {
        log.warn("MethodNotSupported: {} {} | supported={}",
                req.getMethod(), req.getRequestURI(), e.getSupportedHttpMethods());

        String message = String.format(
                "HTTP 메서드가 잘못되었습니다. 사용 불가: %s\n허용된 메서드: %s",
                req.getMethod(),
                e.getSupportedHttpMethods());

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED) // 405
                .body(message);
    }

    /**
     * 오류 코드를 사용자 친화적인 메시지로 변환
     */
    private String translateErrorCode(String code) {
        if (code == null) {
            return "알 수 없는 오류가 발생했습니다.";
        }

        return switch (code) {
            // ===== 회원/사용자 관련 =====
            case "LOGIN_ID_DUPLICATE", "이미 사용 중인 아이디입니다." -> "이미 사용 중인 로그인 ID입니다.";
            case "PHONE_DUPLICATE", "이미 등록된 전화번호입니다." -> "이미 등록된 전화번호입니다.";
            case "EMAIL_DUPLICATE", "이미 등록된 이메일입니다." -> "이미 등록된 이메일입니다.";
            case "USER_NOT_FOUND", "사용자를 찾을 수 없습니다." -> "사용자를 찾을 수 없습니다.";
            case "USER_DELETED" -> "탈퇴한 사용자입니다.";
            case "USER_INACTIVE" -> "비활성화된 계정입니다. 관리자에게 문의해주세요.";
            case "LOGIN_ID_INVALID" -> "로그인 ID가 유효하지 않습니다.";
            case "PASSWORD_INVALID" -> "비밀번호가 유효하지 않습니다.";
            case "NAME_INVALID" -> "이름이 유효하지 않습니다.";
            case "PHONE_INVALID" -> "전화번호가 유효하지 않습니다.";
            case "ROLE_INVALID" -> "역할이 유효하지 않습니다.";
            case "INVALID_STATUS_TRANSITION" -> "잘못된 상태 전환입니다.";

            // ===== 인증/로그인 관련 =====
            case "LOGIN_FAIL" -> "아이디 또는 비밀번호가 일치하지 않습니다.";
            case "TOO_MANY_ATTEMPS" -> "로그인 시도 횟수를 초과했습니다. 잠시 후 다시 시도해주세요.";
            case "INVALID_TOKEN" -> "유효하지 않은 토큰입니다.";
            case "TOKEN_EXPIRED" -> "토큰이 만료되었습니다. 다시 로그인해주세요.";
            case "ALREADY_LOGGED_IN" -> "이미 다른 기기에서 로그인되어 있습니다.";
            case "INVALID_LOGIN_TOKEN" -> "유효하지 않은 로그인 토큰입니다. 다시 시도해주세요.";
            case "SESSION_EXPIRED" -> "세션이 만료되었습니다. 다시 로그인해주세요.";
            case "REFRESH_REUSED" -> "비정상적인 세션 사용이 감지되었습니다. 다시 로그인해주세요.";

            // ===== 휴대폰 인증 관련 =====
            case "PHONE_COOLDOWN" -> "인증번호 재발송 대기 중입니다. 잠시 후 다시 시도해주세요.";
            case "PHONE_DAILY_LIMIT" -> "일일 인증번호 발송 한도를 초과했습니다.";
            case "SMS_SEND_FAILED" -> "SMS 발송에 실패했습니다. 잠시 후 다시 시도해주세요.";
            case "PV_NOT_FOUND" -> "인증 정보를 찾을 수 없습니다.";
            case "PV_NOT_REQUESTED" -> "인증 요청이 없습니다.";
            case "PV_EXPIRED" -> "인증번호가 만료되었습니다. 다시 요청해주세요.";
            case "PV_TOO_MANY_ATTEMPTS" -> "인증 시도 횟수를 초과했습니다. 다시 요청해주세요.";
            case "PV_CODE_INVALID" -> "인증번호가 일치하지 않습니다.";
            case "VERIFICATION_FAILED" -> "인증에 실패했습니다.";
            case "VERIFICATION_EXPIRED" -> "인증 시간이 만료되었습니다.";
            case "INVALID_PROOF_TOKEN" -> "인증 토큰이 만료되었거나 유효하지 않습니다. 다시 인증해주세요.";
            case "PHONE_MISMATCH" -> "인증된 휴대폰 번호가 일치하지 않습니다.";

            // ===== 어르신 관련 =====
            case "ELDERLY_NOT_FOUND", "해당 어르신을 찾을 수 없습니다." -> "어르신 정보를 찾을 수 없습니다.";
            case "ELDERLY_ALREADY_EXISTS" -> "이미 등록된 어르신 정보가 있습니다.";
            case "ROLE_NOT_ELDERLY" -> "어르신 역할이 아닙니다.";
            case "USER_REQUIRED" -> "사용자 정보가 필요합니다.";
            case "ADM_DIVISION_REQUIRED" -> "행정구역 정보가 필요합니다.";
            case "BIRTH_REQUIRED" -> "생년월일 정보가 필요합니다.";
            case "GENDER_REQUIRED" -> "성별 정보가 필요합니다.";
            case "FIELD_TOO_LONG" -> "입력 값이 너무 깁니다.";
            case "HEALTH_INFO_NOT_FOUND" -> "건강 정보를 찾을 수 없습니다.";
            case "ELDERLY_REQUIRED" -> "어르신 정보가 필요합니다.";

            // ===== 보호자 관련 =====
            case "GUARDIAN_NOT_FOUND", "해당 보호자를 찾을 수 없습니다.", "보호자를 찾을 수 없습니다." -> "보호자 정보를 찾을 수 없습니다.";
            case "GUARDIAN_ELDERLY_RELATION_NOT_FOUND" -> "보호자-어르신 연결 정보를 찾을 수 없습니다.";
            case "이미 다른 보호자가 등록한 어르신입니다." -> "이미 다른 보호자가 등록한 어르신입니다.";
            case "이 보호자는 이미 다른 어르신을 담당하고 있습니다." -> "이 보호자는 이미 다른 어르신을 담당하고 있습니다.";
            case "NOT_YOUR_ELDERLY" -> "담당 어르신이 아닙니다.";
            case "연결된 보호자가 아닙니다." -> "연결된 보호자가 아닙니다.";

            // ===== 상담사 관련 =====
            case "COUNSELOR_NOT_FOUND", "해당 상담사를 찾을 수 없습니다." -> "상담사 정보를 찾을 수 없습니다.";
            case "담당 행정구역은 필수입니다." -> "담당 행정구역은 필수입니다.";
            case "사용자 정보는 필수입니다." -> "사용자 정보는 필수입니다.";

            // ===== 배정 관련 =====
            case "ASSIGNMENT_NOT_FOUND" -> "배정 정보를 찾을 수 없습니다.";
            case "ASSIGNMENT_ALREADY_EXISTS" -> "이미 배정된 상담사-어르신 관계입니다.";

            // ===== 행정구역 관련 =====
            case "INVALID_ADM_CODE" -> "유효하지 않은 행정코드입니다.";

            // ===== 민원/문의 관련 =====
            case "COMPLAINT_NOT_FOUND", "민원을 찾을 수 없습니다." -> "민원을 찾을 수 없습니다.";
            case "ALREADY_REPLIED" -> "이미 답변된 민원입니다.";
            case "문의를 찾을 수 없습니다." -> "문의를 찾을 수 없습니다.";
            case "보호자만 1:1 문의를 등록할 수 있습니다." -> "보호자만 1:1 문의를 등록할 수 있습니다.";
            case "상담사만 답변을 등록할 수 있습니다." -> "상담사만 답변을 등록할 수 있습니다.";
            case "본인의 문의만 조회할 수 있습니다." -> "본인의 문의만 조회할 수 있습니다.";

            // ===== 복약 관련 =====
            case "복약 일정을 찾을 수 없습니다." -> "복약 일정을 찾을 수 없습니다.";
            case "어르신 정보를 찾을 수 없습니다." -> "어르신 정보를 찾을 수 없습니다.";
            case "조회 권한이 없습니다." -> "조회 권한이 없습니다.";
            case "삭제 권한이 없습니다." -> "삭제 권한이 없습니다.";
            case "수정 권한이 없습니다." -> "수정 권한이 없습니다.";

            // ===== 공지사항 관련 =====
            case "존재하지 않는 공지사항입니다.", "공지사항 없음" -> "공지사항을 찾을 수 없습니다.";

            // ===== 민감정보 접근 요청 관련 =====
            case "ONLY_GUARDIAN_CAN_REQUEST" -> "보호자만 접근 요청을 할 수 있습니다.";
            case "REQUEST_NOT_FOUND" -> "요청을 찾을 수 없습니다.";
            case "REQUEST_ALREADY_PENDING" -> "이미 처리 대기 중인 요청이 있습니다.";
            case "민감정보 열람 승인이 필요합니다." -> "민감정보 열람 승인이 필요합니다.";

            // ===== 정책 관련 =====
            case "관리자 권한이 없습니다. (등록된 관리자가 아님)" -> "관리자 권한이 없습니다.";
            case "인증 정보가 유효하지 않습니다." -> "인증 정보가 유효하지 않습니다.";

            // ===== 권한 관련 =====
            case "ACCESS_DENIED", "권한이 없습니다." -> "접근 권한이 없습니다.";
            case "UNAUTHORIZED" -> "로그인이 필요합니다.";
            case "담당하지 않는 어르신의 정보에 접근할 수 없습니다." -> "담당하지 않는 어르신의 정보에 접근할 수 없습니다.";
            case "본인이 담당하는 어르신의 보호자 정보만 조회할 수 있습니다." -> "본인이 담당하는 어르신의 보호자 정보만 조회할 수 있습니다.";

            // ===== 일반 오류 =====
            case "INVALID_REQUEST" -> "잘못된 요청입니다.";
            case "REQUIRED_FIELD_MISSING" -> "필수 항목이 누락되었습니다.";
            case "INVALID_RELATION_TYPE" -> "유효하지 않은 관계 유형입니다.";

            // 정의되지 않은 코드는 그대로 반환 (이미 한글이면 그대로, 코드면 코드)
            default -> code.startsWith("USER_") || code.startsWith("PHONE_") ||
                    code.startsWith("PV_") || code.startsWith("ELDERLY_") ||
                    code.startsWith("GUARDIAN_") || code.startsWith("COUNSELOR_") ||
                    code.startsWith("SESSION_") || code.startsWith("LOGIN_") ||
                    code.startsWith("ACCESS_") || code.startsWith("INVALID_") ||
                    code.startsWith("REQUEST_") || code.startsWith("ASSIGNMENT_")
                            ? "처리 중 오류가 발생했습니다. (코드: " + code + ")"
                            : code;
        };
    }

    /**
     * 상태 오류 (IllegalStateException)
     * 이미 로그인 중(BLOCK_NEW), 계정 비활성화 등
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> handleIllegalState(IllegalStateException e) {
        String code = e.getMessage();
        String message = translateStateCode(code);

        log.warn("IllegalStateException: code={}, message={}", code, message);

        // 일부 상태 오류는 401로 처리
        if ("SESSION_EXPIRED".equals(code) || "REFRESH_REUSED".equals(code) || "SESSION_INVALIDATED".equals(code)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", code, "message", message));
        }

        return ResponseEntity.status(HttpStatus.FORBIDDEN) // 403
                .body(Map.of("error", code, "message", message));
    }

    /**
     * 상태 오류 코드를 사용자 친화적인 메시지로 변환
     */
    private String translateStateCode(String code) {
        if (code == null) {
            return "처리할 수 없는 상태입니다.";
        }

        return switch (code) {
            case "ALREADY_LOGGED_IN" -> "이미 다른 기기에서 로그인되어 있습니다.";
            case "SESSION_EXPIRED" -> "세션이 만료되었습니다. 다시 로그인해주세요.";
            case "SESSION_INVALIDATED" -> "다른 기기에서 로그인하여 현재 세션이 종료되었습니다.";
            case "REFRESH_REUSED" -> "비정상적인 세션 사용이 감지되었습니다. 다시 로그인해주세요.";
            case "TOO_MANY_ATTEMPS" -> "로그인 시도 횟수를 초과했습니다. 잠시 후 다시 시도해주세요.";
            case "USER_INACTIVE" -> "비활성화된 계정입니다. 관리자에게 문의해주세요.";
            case "USER_DELETED" -> "탈퇴한 사용자입니다.";
            case "LOGIN_FAIL" -> "아이디 또는 비밀번호가 일치하지 않습니다.";
            case "ELDERLY_ALREADY_EXISTS" -> "이미 등록된 어르신 정보가 있습니다.";
            case "ROLE_NOT_ELDERLY" -> "어르신 역할이 아닙니다.";
            case "REQUEST_ALREADY_PENDING" -> "이미 처리 대기 중인 요청이 있습니다.";
            case "USER_NOT_ACTIVE_STATUS" -> "활성화되지 않은 계정입니다.";
            default -> code.startsWith("USER_") || code.startsWith("SESSION_") ||
                    code.startsWith("LOGIN_") || code.startsWith("ELDERLY_") ||
                    code.startsWith("REQUEST_")
                            ? "처리할 수 없는 상태입니다. (코드: " + code + ")"
                            : code;
        };
    }

    /**
     * Spring Security 권한 거부 예외 처리 (Spring 6.3+)
     */
    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAuthorizationDenied(AuthorizationDeniedException e) {
        log.error("handleAuthorizationDenied: {}", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("접근 권한이 없습니다."));
    }

    /**
     * Spring Security 권한 거부 예외 처리 (레거시 지원)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<?>> handleAccessDenied(AccessDeniedException e) {
        log.error("handleAccessDenied: {}", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("접근 권한이 없습니다."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        // 첫 번째 에러 메시지를 가져옵니다 (예: "비밀번호는 필수입니다.")
        String errorMessage = ex.getBindingResult().getAllErrors().get(0).getDefaultMessage();

        log.warn("Validation Failed: {}", errorMessage);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST) // 400
                .body(ApiResponse.error(errorMessage));
    }

}
