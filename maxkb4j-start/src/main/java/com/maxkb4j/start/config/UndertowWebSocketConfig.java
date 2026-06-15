package com.maxkb4j.start.config;

import io.undertow.server.DefaultByteBufferPool;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;
import org.springframework.boot.web.embedded.undertow.UndertowDeploymentInfoCustomizer;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Configuration;

/**
 * 显式为 Undertow WebSocket 容器设置 ByteBufferPool，
 * 避免启动时出现警告：
 * "UT026010: Buffer pool was not set on WebSocketDeploymentInfo, the default pool will be used"
 */
@Configuration
public class UndertowWebSocketConfig
        implements WebServerFactoryCustomizer<UndertowServletWebServerFactory> {

    /** 默认缓冲区大小（字节）。1024 与 Undertow 内置默认池一致，足以承载普通 WebSocket 帧 */
    private static final int BUFFER_SIZE = 1024;

    /** 是否使用堆外内存。开启后减少 GC 压力，与 Undertow 默认行为一致 */
    private static final boolean DIRECT_BUFFERS = true;

    @Override
    public void customize(UndertowServletWebServerFactory factory) {
        factory.addDeploymentInfoCustomizers(new WebSocketBufferPoolCustomizer());
    }

    private static class WebSocketBufferPoolCustomizer implements UndertowDeploymentInfoCustomizer {
        @Override
        public void customize(DeploymentInfo deploymentInfo) {
            WebSocketDeploymentInfo wsInfo = (WebSocketDeploymentInfo) deploymentInfo
                    .getServletContextAttributes()
                    .get(WebSocketDeploymentInfo.ATTRIBUTE_NAME);
            if (wsInfo == null) {
                wsInfo = new WebSocketDeploymentInfo();
                deploymentInfo.addServletContextAttribute(
                        WebSocketDeploymentInfo.ATTRIBUTE_NAME, wsInfo);
            }
            wsInfo.setBuffers(new DefaultByteBufferPool(DIRECT_BUFFERS, BUFFER_SIZE));
        }
    }
}
