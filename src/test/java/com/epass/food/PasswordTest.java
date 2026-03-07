package com.epass.food;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码测试类
 */
class PasswordTest {

    /**
     * 测试密码哈希
     */
    @Test
    void testPasswordHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String encode = encoder.encode("User1@123");
        System.out.println("哈希结果: " + encode);
    }
}