package com.xin.aiagent.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
        if (StringUtils.hasText(auth) && auth.startsWith(prefix)) {
            String token = auth.substring(prefix.length());
            try {
                Claims claims = jwtTokenProvider.parse(token);
                String username = claims.getSubject();
                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) claims.getOrDefault("roles", List.of("ROLE_USER"));
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                for (String r : roles) {
                    authorities.add(new SimpleGrantedAuthority(r));
                }
                var authentication = new UsernamePasswordAuthenticationToken(username, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception ignored) {
                // 解析失败：清理上下文，交由授权规则处理
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
