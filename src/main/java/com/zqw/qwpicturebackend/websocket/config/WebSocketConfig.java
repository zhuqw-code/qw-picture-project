package com.zqw.qwpicturebackend.websocket.config;

import com.zqw.qwpicturebackend.websocket.PictureWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import javax.annotation.Resource;

@Configuration
@Slf4j
public class WebSocketConfig implements WebSocketConfigurer {

    @Resource
    private PictureWebSocketHandler pictureWebSocketHandler;

    @Resource
    private WsHandshakeInterceptor wSHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        log.info("websocket 加载websocket:{}", "pictureWebSocketHandler");
        registry.addHandler(pictureWebSocketHandler, "/ws/picture/edit")
                .addInterceptors(wSHandshakeInterceptor)
                .setAllowedOrigins("*");
    }
}
