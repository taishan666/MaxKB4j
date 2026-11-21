package com.maxkb4j.common.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author tarzan
 */
@ConfigurationProperties("system")
@Component
@Data
public class SystemProperties {
    private String defaultUsername;
    private String defaultPassword;
    private String defaultPhone;
    private String defaultEmail;
    /**
     * 可信反向代理地址（精确 IP 或 IPv4 CIDR，如 172.16.0.0/12）。
     * 仅当请求 TCP 对端命中此列表或为回环地址时，WebUtil.getIP 才采信 X-Forwarded-For / X-Real-IP。
     */
    private List<String> trustedProxies = new ArrayList<>();

}
