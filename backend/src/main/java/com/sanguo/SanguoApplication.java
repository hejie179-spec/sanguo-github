package com.sanguo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.sanguo.mapper")
public class SanguoApplication {

    // 后端应用的启动入口，相当于整个后端的开关
    // @MapperScan 注解是告诉系统去哪里找数据库访问的接口
    // 所有的接口都有统一的前缀，具体配置在 application.yml 文件里
    public static void main(String[] args) {
        SpringApplication.run(SanguoApplication.class, args);
    }
}
