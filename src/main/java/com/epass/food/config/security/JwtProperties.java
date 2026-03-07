package com.epass.food.config.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 配置类 读取配置文件 auth.jwt 相关的属性
 *
 * @ConfigurationProperties(prefix = "auth.jwt") 读取配置文件 auth.jwt 相关的属性
 */
@Data
@ConfigurationProperties(prefix = "auth.jwt")
public class JwtProperties {

    private String secret;
    private Long expireSeconds;
}