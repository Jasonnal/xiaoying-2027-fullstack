package com.xiaoying.studio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MaterialStudioApplication {

    public static void main(String[] args) {
        // 结果整理阶段会在无显示环境下用 Java2D 画图
        System.setProperty("java.awt.headless", "true");
        SpringApplication.run(MaterialStudioApplication.class, args);
    }
}
