package com.epass.food.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration // 配置类
@EnableWebSecurity // 启用 Spring Security 的 Web 安全配置
public class SecurityConfig {

    // SecurityFilterChain 定义请求经过哪些安全规则
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/test/**", "/auth/login").permitAll() // 允许访问的路径
                        .anyRequest().authenticated() // 其他请求都需要认证
                )
                .formLogin(AbstractHttpConfigurer::disable); // 关闭默认登录页

        return http.build();
    }
}