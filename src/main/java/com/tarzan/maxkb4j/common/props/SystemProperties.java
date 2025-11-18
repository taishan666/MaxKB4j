package com.tarzan.maxkb4j.common.props;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author tarzan
 */
@ConfigurationProperties("system")
@Component
@Data
public class SystemProperties {
    private String defaultUsername;
    private String defaultPassword;

}
