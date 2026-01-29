package com.authvs.server.config;

import com.authvs.server.handler.RestAccessDeniedHandler;
import com.authvs.server.handler.RestAuthenticationEntryPoint;
import com.authvs.server.model.out.R;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class DefaultSecurityConfig {

    @Autowired
    private RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    @Autowired
    private RestAccessDeniedHandler restAccessDeniedHandler;

    /**
     * 默认安全过滤器链，处理普通 Web 请求（如登录页面）
     */
    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/assets/**", "/webjars/**", "/login", "/favicon.ico", "/error", "/", "/api/captcha", "/api/login/**", "/api/login").permitAll()
                        .anyRequest().authenticated()
                )
                // 异常处理
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler)
                )
                // 禁用 CSRF (前后端分离通常使用 Token，或者如果使用 Session 需配合 Cookie)
                // 这里暂时保持开启或根据需求禁用。如果是纯 API 分离，通常禁用 CSRF 或通过 Header 传递。
                // 为了简化开发和避免 CSRF Token 问题，暂时禁用。
                .csrf(AbstractHttpConfigurer::disable)
                // 启用 CORS
                // .cors(cors -> cors.configurationSource(corsConfigurationSource));
                // 使用全局 CorsFilter (Ordered.HIGHEST_PRECEDENCE) 处理跨域，此处不再重复配置
                .cors(AbstractHttpConfigurer::disable)
                .logout(logout -> logout
                        .logoutUrl("/api/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(new ObjectMapper().writeValueAsString(R.ok()));
                        })
                );

        return http.build();
    }

    /**
     * 暴露 AuthenticationManager，用于自定义登录接口
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * 密码加密器 BCrypt
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
