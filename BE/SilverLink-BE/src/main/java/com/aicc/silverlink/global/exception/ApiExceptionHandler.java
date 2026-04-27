package com.aicc.silverlink.global.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(LuxiaHttpException.class)
    public ResponseEntity<?> handleLuxia(LuxiaHttpException e) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", "UPSTREAM_LUXIA_ERROR");
        body.put("upstreamStatus", e.status());
        body.put("upstreamBody", e.body());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
    }
}
