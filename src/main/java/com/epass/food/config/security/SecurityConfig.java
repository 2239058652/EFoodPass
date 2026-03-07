package com.epass.food.config.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 配置类
 * 配置 Spring Security 的过滤器链，并定义请求的访问权限
 * 创建 SecurityFilterChain Bean，用于配置 Spring Security 的过滤器链
 * 创建 PasswordEncoder Bean，用于密码加密
 *
 * @Configuration 标记当前类为配置类
 * @EnableWebSecurity 启用 Spring Security 的 Web 安全配置
 * @EnableConfigurationProperties(JwtProperties.class) 启用 JwtProperties 类的属性配置
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

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

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}