package com.xin.aiagent.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JWT 认证过滤器：从请求头解析 JWT 并注入认证上下文。
 *
 * 仅在有效 Token 存在时设置认证信息，不拦截链路，错误时清空上下文并放行，
 * 由后续的授权规则决定是否返回 401/403。
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final String header;
    private final String prefix;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   @Value("${jwt.header}") String header,
                                   @Value("${jwt.token-prefix}") String prefix) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.header = header;
        this.prefix = prefix.trim() + " ";
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String auth = request.getHeader(header);
        if (log.isTraceEnabled()) {
            log.trace("JWT filter: path={}, headerName={}, hasAuthHeader={}, method={}",
                    request.getRequestURI(), header, StringUtils.hasText(auth), request.getMethod());
        }
        if (StringUtils.hasText(auth) && auth.startsWith(prefix)) {
            String token = auth.substring(prefix.length());
            try {
                Claims claims = jwtTokenProvider.parse(token);
                String username = claims.getSubject();
                Long userId = ((Number) claims.get("uid")).longValue();
                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) claims.getOrDefault("roles", List.of("ROLE_USER"));
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                for (String r : roles) {
                    authorities.add(new SimpleGrantedAuthority(r));
                }
                UserPrincipal principal = new UserPrincipal(userId, username);
                var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
                if (log.isDebugEnabled()) {
                    log.debug("JWT authenticated: userId={}, username={}, roles={}", userId, username, roles);
                }
            } catch (Exception ignored) {
                // 解析失败：清理上下文，交由授权规则处理
                SecurityContextHolder.clearContext();
                if (log.isWarnEnabled()) {
                    log.warn("JWT parse failed or invalid token. path={}, reason={}", request.getRequestURI(), ignored.getMessage());
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
