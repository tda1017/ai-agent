package com.xin.aiagent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xin.aiagent.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局安全配置：启用基于 JWT 的无状态认证与接口授权策略。
 */
@Configuration
@EnableMethodSecurity
@Slf4j
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtFilter) throws Exception {
        http
                // 使用 JWT，无需 CSRF 保护与服务端会话
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(reg -> reg
                        // 认证白名单（登录/注册/文档/错误页/静态资源等）
                        .requestMatchers(
                                "/api/auth/**",
                                "/doc.html", "/swagger-ui/**", "/v3/api-docs/**",
                                "/error", "/", "/index.html", "/static/**"
                        ).permitAll()
                        // 预检请求放行
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        // 其他 API 需认证
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )
                // 未认证时返回 401 而不是 403
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            if (log.isDebugEnabled()) {
                                log.debug("AuthenticationEntryPoint: path={}, method={}, msg={}",
                                        request.getRequestURI(), request.getMethod(), authException.getMessage());
                            }
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");

                            Map<String, Object> body = new HashMap<>();
                            body.put("code", 401);
                            body.put("message", "未认证或 token 已过期，请重新登录");

                            new ObjectMapper().writeValue(response.getOutputStream(), body);
                        })
                )
                // 在用户名密码过滤器之前加入 JWT 解析注入
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 全局 CORS 配置：允许本地前端(5173)进行跨域访问包含 DELETE 在内的所有常用方法。
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        // 本地开发常见来源；如需生产请收敛域名
        cfg.setAllowedOrigins(java.util.List.of(
                "http://localhost:5173",
                "http://127.0.0.1:5173"
        ));
        cfg.setAllowedMethods(java.util.List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        cfg.setAllowedHeaders(java.util.List.of("*"));
        cfg.setExposedHeaders(java.util.List.of("*"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 仅开放 API 路径，避免误开放静态文件
        source.registerCorsConfiguration("/api/**", cfg);
        return source;
    }
}
