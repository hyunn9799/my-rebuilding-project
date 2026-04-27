package com.aicc.silverlink.domain.terms.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "약관", description = "이용약관 API")
@RestController
@RequestMapping("/api/terms")
public class TermsController {
}
