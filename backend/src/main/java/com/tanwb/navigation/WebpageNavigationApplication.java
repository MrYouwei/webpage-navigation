package com.tanwb.navigation;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.tanwb.navigation.mapper")
public class WebpageNavigationApplication {
    public static void main(String[] args) {
        SpringApplication.run(WebpageNavigationApplication.class, args);
    }
}
