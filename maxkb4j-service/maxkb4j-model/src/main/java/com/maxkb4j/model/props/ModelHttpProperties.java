package com.maxkb4j.model.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 模型调用 HTTP 客户端配置（对所有模型供应商生效）
 * <p>
 * 可通过 application.yml 中 maxkb.model.http.* 覆盖。
 */
@ConfigurationProperties(prefix = "maxkb.model.http")
@Component
@Data
public class ModelHttpProperties {

    /** 建立连接超时时间（毫秒），默认 60s；设为 0 表示不限制 */
    private long connectTimeout = 60_000;

    /**
     * Socket 读超时（毫秒），即流式(SSE)响应中两次数据之间的最大等待间隔，默认 30 分钟；设为 0 表示不限制。
     * <p>
     * 注意：读超时不是请求总时长。思考/推理类模型在生成前可能长时间不返回任何 SSE 数据，
     * 该值过小会触发 {@code java.net.SocketTimeoutException: Read timed out} 中断整段流式输出。
     */
    private long readTimeout = 1_800_000;
}
