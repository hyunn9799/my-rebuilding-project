package com.aicc.silverlink.domain.welfare.dto;

import com.aicc.silverlink.domain.welfare.entity.Welfare;
import lombok.*;

/**
 * 복지 서비스 상세 조회 응답 DTO
 * 요구사항 #20, #25 대응
 */
@Getter
@Setter
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class WelfareDetailResponse {
    private Long id;
    private String servNm;
    private String source;
    private String jurMnofNm;
    private String servDgst;
    private String targetDtlCn;
    private String slctCritCn;
    private String alwServCn;
    private String rprsCtadr;
    private String servDtlLink;
    private String category;
}