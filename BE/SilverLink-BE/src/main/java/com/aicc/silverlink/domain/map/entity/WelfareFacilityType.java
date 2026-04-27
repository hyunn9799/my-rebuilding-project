package com.aicc.silverlink.domain.map.entity;

public enum WelfareFacilityType {
    ELDERLY_WELFARE_CENTER("노인복지관"),
    DISABLED_WELFARE_CENTER("장애인복지관"),
    CHILD_WELFARE_CENTER("아동복지관"),
    COMMUNITY_WELFARE_CENTER("종합사회복지관"),
    SENIOR_CENTER("경로당"),
    DAYCARE_CENTER("주간보호센터"),
    HOME_CARE_SERVICE("재가복지서비스");

    private final String description;

    WelfareFacilityType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
