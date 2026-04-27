//package com.aicc.silverlink.global.security.principal;
//
//import com.aicc.silverlink.domain.user.entity.Role;
//import com.aicc.silverlink.domain.user.entity.User;
//import lombok.Getter;
//import lombok.RequiredArgsConstructor;
//import org.jetbrains.annotations.NotNull;
//import org.springframework.security.core.GrantedAuthority;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.userdetails.UserDetails;
//
//import java.util.Collection;
//import java.util.Collections;
//import java.util.List;
//
//@Getter // 👈 Lombok Getter 필수 (getUserId 자동 생성)
//@RequiredArgsConstructor
//public class UserPrincipal implements UserDetails {
//
//    private final Long userId;
//    private final String loginId;
//    private final String password;
//    private final Role role;
//
//    // 엔티티 -> Principal 변환용 생성자 (정적 팩토리)
//    public static UserPrincipal from(User user) {
//        return new UserPrincipal(
//                user.getId(),
//                user.getLoginId(),
//                user.getPassword(),
//                user.getRole()
//        );
//    }
//
//    @NotNull
//    @Override
//    public Collection<? extends GrantedAuthority> getAuthorities() {
//        // "ROLE_" 접두사 붙이는 게 스프링 시큐리티 국룰
//        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
//    }
//
//    @Override
//    public String getPassword() { return password; }
//
//    @NotNull
//    @Override
//    public String getUsername() { return loginId; }
//
//    // 계정 만료/잠금 여부 등 (일단 모두 true로 설정)
//    @Override public boolean isAccountNonExpired() { return true; }
//    @Override public boolean isAccountNonLocked() {
//        return UserDetails.super.isAccountNonLocked();
//    }
//    @Override public boolean isCredentialsNonExpired() {
//        return UserDetails.super.isCredentialsNonExpired();
//    }
//    @Override public boolean isEnabled() {
//        return UserDetails.super.isEnabled();
//    }
//}