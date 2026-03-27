package com.zqw.qwpicturebackend;

import org.apache.shardingsphere.spring.boot.ShardingSphereAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication(exclude = {ShardingSphereAutoConfiguration.class})
@MapperScan("com.zqw.qwpicturebackend.mapper")   // mybatis-plus扫描mapper文件
@EnableAspectJAutoProxy(exposeProxy = true)
public class QwPictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(QwPictureBackendApplication.class, args);
    }

}
