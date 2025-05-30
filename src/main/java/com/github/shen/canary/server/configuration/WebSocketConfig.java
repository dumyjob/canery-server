package com.github.shen.canary.server.configuration;

import com.github.shen.canary.server.web.TaskController;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * @see TaskController
 */
// WebSocket配置类
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {


    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 通过 registerStompEndpoints 方法定义客户端连接端点，并支持 SockJS 兼容性：
        registry.addEndpoint("/ws-logs").setAllowedOrigins("*").withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        //通过 configureMessageBroker 方法设置消息代理前缀和应用程序目的地前缀
        registry.enableSimpleBroker("/topic");
        ; // 代理转发以 /topic 开头的消息
        registry.setApplicationDestinationPrefixes("/app"); // 客户端发送消息的前缀
    }
}