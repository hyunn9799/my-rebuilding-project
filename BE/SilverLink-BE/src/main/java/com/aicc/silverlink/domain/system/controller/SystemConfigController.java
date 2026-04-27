package com.aicc.silverlink.domain.system.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "시스템 설정 (관리자)", description = "시스템 설정 관리 API (관리자 전용)")
@RestController
@RequestMapping("/api/admin/system-config")
public class SystemConfigController {
}
