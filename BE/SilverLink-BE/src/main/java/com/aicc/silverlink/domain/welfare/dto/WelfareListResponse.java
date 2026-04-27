package com.aicc.silverlink.domain.welfare.dto;

import com.aicc.silverlink.domain.welfare.entity.Welfare;
import lombok.*;

@Getter
@Setter // ModelMapper가 값을 채우기 위해 필요
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WelfareListResponse {
    private Long id;
    private String servNm;
    private String source; // ModelMapper가 Source Enum의 name을 자동으로 String에 매핑함
    private String jurMnofNm;
    private String category;
    private String servDgst;
    private String rprsCtadr; // 문의처

    // Getter에서 가공 로직을 처리하면 ModelMapper를 써도 UI 대응이 가능합니다.
    public String getServDgst() {
        if (this.servDgst != null && this.servDgst.length() > 50) {
            return this.servDgst.substring(0, 50) + "...";
        }
        return this.servDgst;
    }
}