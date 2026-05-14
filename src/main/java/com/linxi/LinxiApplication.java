package com.linxi;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 林夕置业酒店经营日报管理系统
 */
@SpringBootApplication
@MapperScan("com.linxi.mapper")
@EnableScheduling
public class LinxiApplication {

    public static void main(String[] args) {
        SpringApplication.run(LinxiApplication.class, args);
        System.out.println("========================================");
        System.out.println("  林夕置业经营日报系统启动成功！");
        System.out.println("  API文档: http://localhost:8080/doc.html");
        System.out.println("========================================");
    }
}
