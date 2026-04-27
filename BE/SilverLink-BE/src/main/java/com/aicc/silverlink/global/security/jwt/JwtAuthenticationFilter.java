package com.aicc.silverlink.global.security.jwt;

import com.aicc.silverlink.domain.session.service.SessionService;
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.global.exception.UnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwt;
    private final SessionService sessionService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // ✅ 이미 인증이 있으면 (예: @WithMockUser 테스트) 건너뛰기
        Authentication existingAuth = SecurityContextHolder.getContext().getAuthentication();
        if (existingAuth != null && existingAuth.isAuthenticated()
                && !"anonymousUser".equals(existingAuth.getPrincipal())) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = resolveBearer(request);

        if (token != null) {
            try {
                Jws<Claims> jws = jwt.parseAndValidate(token); // 토큰 서명 검증 + 만료 체크

                Claims claims = jws.getPayload();

                String userIdStr = claims.getSubject();
                String roleStr = claims.get("role", String.class);
                String sid = claims.get("sid", String.class);

                Long userId = Long.valueOf(userIdStr);
                Role role = Role.valueOf(roleStr);

                if (!sessionService.isActive(sid, userId)) {
                    // 강제 종료된 것인지 vs 만료된 것인지 구분
                    if (sessionService.wasInvalidated(sid)) {
                        logger.warn("Session kicked (다른 기기에서 로그인) for sid: " + sid);
                        response.setHeader("X-Session-Status", "KICKED");
                    } else {
                        logger.warn("Session expired or invalid for sid: " + sid);
                    }
                    SecurityContextHolder.clearContext();
                    throw new UnauthorizedException("세션이 만료되었습니다.");
                }

                // idle 연장
                sessionService.touch(sid);

                var auth = new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role.name())));

                // ✅ SecurityContext에 인증 설정
                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (ExpiredJwtException e) {
                logger.warn("JWT Token Expired: " + e.getMessage());
            } catch (SignatureException e) {
                logger.error("JWT Signature Invalid: " + e.getMessage());
            } catch (MalformedJwtException e) {
                logger.error("JWT Malformed: " + e.getMessage());
            } catch (JwtException | IllegalArgumentException e) {
                logger.error("Invalid JWT Token: " + e.getMessage());
            } catch (Exception e) {
                logger.error("Authentication Error: " + e.getMessage());
                e.printStackTrace();
            }
        }
        filterChain.doFilter(request, response);
    }

    private String resolveBearer(HttpServletRequest req) {
        String h = req.getHeader("Authorization");
        if (h == null || h.isBlank())
            return null;

        if (!h.startsWith("Bearer "))
            return null;

        String token = h.substring(7).trim();
        return token.isEmpty() ? null : token;
    }

}
