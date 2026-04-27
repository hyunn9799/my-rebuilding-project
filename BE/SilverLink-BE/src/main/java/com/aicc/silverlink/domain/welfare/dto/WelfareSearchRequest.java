package com.aicc.silverlink.domain.welfare.dto;

import com.aicc.silverlink.domain.welfare.entity.Source;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class WelfareSearchRequest {

    @Size(min = 2, message = "검색어는 최소 2자 이상 입력해주세요.")
    private String keyword;

    private String districtCode;

    private String category;

    private Source source; // 출처 필터

    private int page = 0;        // 요청 페이지 번호 (기본값 0)
    private int size = 10;       // 한 페이지당 보여줄 개수
}
