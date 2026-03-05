package com.epass.food;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * E食通 - 主启动类
 * -
 * -@SpringBootApplication 是一个组合注解，包含：
 * - @Configuration: 标记为配置类
 * - @EnableAutoConfiguration: 启用自动配置
 * - @ComponentScan: 自动扫描组件
 */
@SpringBootApplication
@MapperScan("com.epass.food.modules.*.mapper") // 使用通配符 * 匹配中间的模块名，精确锁定到 mapper 包
public class EFoodPassApplication {

    public static void main(String[] args) {
        SpringApplication.run(EFoodPassApplication.class, args);

        System.out.println("""
                
                ╔═════════════════════════════════════════════════════════════╗
                ║                                                             ║
                ║   🏥 EFoodPass (E食通) Started Successfully! 🏥       ║
                ║                                                             ║
                ║   🌐 Server: http://localhost:5603                          ║
                ║   📚 Swagger: http://localhost:5603/swagger-ui/index.html#/ ║
                ║                                                             ║
                ╚═════════════════════════════════════════════════════════════╝
                """);
    }

}
