package com.aicc.silverlink.domain.guardian.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuardianRequest {

    private String loginId;
    private String password;
    private String name;
    private String email;
    private String phone;

    private String addressLine1;
    private String addressLine2;
    private String zipcode;


}
