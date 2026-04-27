package com.aicc.silverlink.domain.user.entity;

public enum Role {
    ADMIN,COUNSELOR,GUARDIAN,ELDERLY;

    public String asAuthority(){
        return "ROLE_" + name();
    }
}
