package com.xin.aiagent.config;

import com.xin.aiagent.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 全局安全配置：启用基于 JWT 的无状态认证与接口授权策略。
 */
@Configuration
@EnableMethodSecurity
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
                // 在用户名密码过滤器之前加入 JWT 解析注入
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
