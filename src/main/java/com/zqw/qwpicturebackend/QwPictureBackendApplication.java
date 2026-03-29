package com.zqw.qwpicturebackend;

import com.zqw.qwpicturebackend.websocket.config.WebSocketConfig;
import org.apache.shardingsphere.spring.boot.ShardingSphereAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.web.socket.config.annotation.EnableWebSocket;

@SpringBootApplication(exclude = {ShardingSphereAutoConfiguration.class})
@MapperScan("com.zqw.qwpicturebackend.mapper")   // mybatis-plus扫描mapper文件
@EnableAspectJAutoProxy(exposeProxy = true)
@EnableWebSocket
public class QwPictureBackendApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(QwPictureBackendApplication.class, args);
        // 强行获取 Bean，测试是否存在
        // try {
        //     WebSocketConfig config = context.getBean(WebSocketConfig.class);
        //     System.out.println(config);
        //     System.out.println("========== 成功获取到 WebSocketConfig ==========");
        // } catch (Exception e) {
        //     System.out.println("========== 未找到 WebSocketConfig！==========");
        //     e.printStackTrace();
        // }
    }

}
